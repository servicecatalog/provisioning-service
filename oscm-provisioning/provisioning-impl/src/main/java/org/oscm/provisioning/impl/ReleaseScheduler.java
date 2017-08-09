/*
 * ****************************************************************************
 *
 *    Copyright FUJITSU LIMITED 2017
 *
 *    Creation Date: 2017-08-07
 *
 * ****************************************************************************
 */

package org.oscm.provisioning.impl;

import akka.Done;
import akka.actor.ActorSystem;
import akka.persistence.cassandra.session.javadsl.CassandraSession;
import akka.stream.Materializer;
import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.lightbend.lagom.javadsl.persistence.*;
import com.lightbend.lagom.javadsl.persistence.cassandra.CassandraReadSide;
import org.oscm.provisioning.impl.data.*;
import org.oscm.provisioning.impl.data.ReleaseCommand.InternalCommitRelease;
import org.oscm.provisioning.impl.data.ReleaseCommand.InternalFailRelease;
import org.oscm.rudder.api.RudderService;
import org.pcollections.PSequence;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import static com.lightbend.lagom.javadsl.persistence.cassandra.CassandraReadSide.completedStatement;

@Singleton
public class ReleaseScheduler {

    private final RudderClientManager rudderClientManager;
    private final CassandraSession session;
    private final PersistentEntityRegistry registry;
    private final Materializer materializer;

    @Inject
    public ReleaseScheduler(RudderClientManager rudderClientManager,
        CassandraSession session, ActorSystem system, ReadSide readSide,
        PersistentEntityRegistry registry, Materializer materializer) {

        this.rudderClientManager = rudderClientManager;
        this.session = session;
        this.registry = registry;
        this.materializer = materializer;

        FiniteDuration executionInterval = FiniteDuration
            .fromNanos(system.settings().config()
                .getDuration(Config.WATCHDOG_EXECUTION_INTERVAL).toNanos());

        FiniteDuration monitorInterval = FiniteDuration
            .fromNanos(system.settings().config()
                .getDuration(Config.WATCHDOG_MONITOR_INTERVAL).toNanos());

        readSide.register(ReleaseProcessor.class);

        system.scheduler()
            .schedule(Duration.create(0, TimeUnit.SECONDS), executionInterval,
                this::executeReleases, system.dispatcher());
        system.scheduler()
            .schedule(Duration.create(0, TimeUnit.SECONDS), monitorInterval,
                this::monitorReleases, system.dispatcher());
    }

    private void executeReleases() {

        serveRelease(ReleaseStatus.INSTALLING, ReleaseStatus.UPDATING,
            ReleaseStatus.DELETING, ReleaseStatus.PENDING);
    }

    private void monitorReleases() {
        serveRelease(ReleaseStatus.DEPLOYED);
    }

    private void serveRelease(ReleaseStatus... statuses) {
        try {
            //TODO add tags to binding
            session.select(
                "SELECT id, status FROM releaseSchedule WHERE tag IN ? AND status IN ?",
                statuses)
                .runForeach(row -> {
                    UUID id = row.getUUID("id");
                    ReleaseStatus status = row
                        .get("status", ReleaseStatus.class);

                    PersistentEntityRef<ReleaseCommand> ref = registry
                        .refFor(ReleaseEntity.class, id.toString());

                    ref.ask(ReleaseCommand.InternalGetReleaseState.INSTANCE).thenCompose(
                        state -> {
                            Release release = state.getRelease();
                            String instance = state.getInstance();

                            URI target;
                            try {
                                target = new URI(release.getTarget());
                            } catch (URISyntaxException e) {
                                //TODO add custom exception
                                return ref.ask(new InternalFailRelease(null));
                            }

                            RudderService service = rudderClientManager.getServiceForURI(target);

                            switch (status) {
                            case INSTALLING:
                                return service.install()
                                    .invoke(
                                        release.getAsInstallRequest(state.getInstance()))
                                    .thenCompose(
                                        notUsed ->
                                            ref.ask(new InternalCommitRelease(null)))
                                    .exceptionally(
                                        throwable -> {
                                            //TODO add custom exception
                                            ref.ask(new InternalFailRelease(null));
                                            return Done.getInstance();
                                        });
                            case UPDATING:
                                return service.update()
                                    .invoke(
                                        release.getAsUpdateRequest(state.getInstance()))
                                    .thenCompose(
                                        notUsed -> ref
                                            .ask(new ReleaseCommand.InternalCommitRelease(null)))
                                    .exceptionally(
                                        throwable -> {
                                            //TODO add custom exception
                                            ref.ask(new InternalFailRelease(null));
                                            return Done.getInstance();
                                        });
                            case DELETING:
                                return service.delete(instance)
                                    .invoke().thenCompose(
                                        notUsed -> ref
                                            .ask(new ReleaseCommand.InternalCommitRelease(null)))
                                    .exceptionally(
                                        throwable -> {
                                            //TODO add custom exception
                                            ref.ask(new InternalFailRelease(null));
                                            return Done.getInstance();
                                        });
                            case PENDING:
                                return service.status(instance, release.getVersion())
                                    .invoke()
                                    .thenCompose(
                                        response -> {
                                            Map<String, String> services = extractServices(
                                                response.getInfo().getStatus().getResources());

                                            return ref.ask(
                                                new ReleaseCommand.InternalCommitRelease(services));
                                        })
                                    .exceptionally(
                                        throwable -> {
                                            //TODO add custom exception
                                            ref.ask(new InternalFailRelease(null));
                                            return Done.getInstance();
                                        });
                            case DEPLOYED:
                                return service.status(instance, release.getVersion())
                                    .invoke()
                                    .thenCompose(
                                        response -> {

                                            return ref.ask(
                                                new ReleaseCommand.InternalFailRelease(null));
                                        })
                                    .exceptionally(
                                        throwable -> {
                                            //TODO log error
                                            return Done.getInstance();
                                        });

                            default:
                                return CompletableFuture.completedFuture(Done.getInstance());
                            }

                        });

                }, materializer).exceptionally(throwable -> {
                //TODO Log error
                return Done.getInstance();
            });
        } catch (IllegalStateException ise) {
            //ignore
        }
    }

    private Map<String, String> extractServices(String resources) {
        //TODO
        return Collections.emptyMap();
    }

    public static class ReleaseProcessor
        extends ReadSideProcessor<ReleaseEvent> {

        private static final String OFFSET_ID = "release-processor-offset";

        private final CassandraReadSide readSide;
        private final CassandraSession session;

        private PreparedStatement insertStatement;
        private PreparedStatement updateStatement;
        private PreparedStatement deleteStatement;

        @Inject
        public ReleaseProcessor(CassandraReadSide readSide,
            CassandraSession session) {
            this.readSide = readSide;
            this.session = session;
        }

        @Override
        public ReadSideHandler<ReleaseEvent> buildHandler() {
            return readSide.<ReleaseEvent>builder(OFFSET_ID)
                .setGlobalPrepare(this::createTable)
                .setPrepare(tag -> prepareInsertStatement()
                    .thenCompose(d -> prepareUpdateStatement()
                        .thenCompose(d2 -> prepareDeleteStatement())))
                .setEventHandler(ReleaseEvent.InstallingRelease.class,
                    evt -> insert(evt, ReleaseStatus.INSTALLING))
                .setEventHandler(ReleaseEvent.UpdatingRelease.class,
                    evt -> update(evt, ReleaseStatus.UPDATING))
                .setEventHandler(ReleaseEvent.DeletingRelease.class,
                    evt -> update(evt, ReleaseStatus.DELETING))
                .setEventHandler(ReleaseEvent.PendingRelease.class,
                    evt -> update(evt, ReleaseStatus.PENDING))
                .setEventHandler(ReleaseEvent.DeployedRelease.class,
                    evt -> update(evt, ReleaseStatus.DEPLOYED))
                .setEventHandler(ReleaseEvent.ErrorRelease.class,
                    evt -> update(evt, ReleaseStatus.ERROR))
                .setEventHandler(ReleaseEvent.DeletedRelease.class,
                    this::delete)
                .setEventHandler(ReleaseEvent.FailedRelease.class,
                    this::delete)
                .build();
        }

        private CompletionStage<Done> createTable() {
            return session.executeCreateTable(
                "CREATE TABLE IF NOT EXISTS releaseSchedule ( " +
                    "id uuid, " +
                    "tag string, " +
                    "status string, " +
                    "PRIMARY KEY (id)" +
                    ")").thenCompose(d ->
                session.executeCreateTable(
                    "CREATE INDEX IF NOT EXISTS releaseScheduleIndex " +
                        "ON releaseSchedule (tag)")
            );
        }

        private CompletionStage<Done> prepareInsertStatement() {
            return session.prepare(
                "INSERT INTO releaseSchedule(id, tag, status) VALUES (?, ?, ?)")
                .thenApply(s -> {
                    insertStatement = s;
                    return Done.getInstance();
                });
        }

        private CompletionStage<Done> prepareUpdateStatement() {
            return session.prepare(
                "UPDATE releaseSchedule SET tag = ?, status = ? WHERE id = ?")
                .thenApply(s -> {
                    updateStatement = s;
                    return Done.getInstance();
                });
        }

        private CompletionStage<Done> prepareDeleteStatement() {
            return session.prepare("DELETE FROM releaseSchedule WHERE id = ?")
                .thenApply(s -> {
                    deleteStatement = s;
                    return Done.getInstance();
                });
        }

        private CompletionStage<List<BoundStatement>> insert(
            ReleaseEvent event, ReleaseStatus status) {

            return completedStatement(insertStatement.bind(event.getId(),
                ReleaseEvent.TAG.forEntityId(event.getIdString()).tag(), status
            ));
        }

        private CompletionStage<List<BoundStatement>> update(
            ReleaseEvent event, ReleaseStatus status) {

            return completedStatement(updateStatement.bind(
                ReleaseEvent.TAG.forEntityId(event.getIdString()).tag(), status, event.getId()
            ));
        }

        private CompletionStage<List<BoundStatement>> delete(
            ReleaseEvent event) {
            return completedStatement(deleteStatement.bind(event.getId()));
        }

        @Override
        public PSequence<AggregateEventTag<ReleaseEvent>> aggregateTags() {
            return ReleaseEvent.TAG.allTags();
        }
    }

}
