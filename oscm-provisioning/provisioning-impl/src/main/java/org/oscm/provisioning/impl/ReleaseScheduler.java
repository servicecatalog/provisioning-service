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
import akka.stream.Materializer;
import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.lightbend.lagom.javadsl.api.transport.TransportErrorCode;
import com.lightbend.lagom.javadsl.api.transport.TransportException;
import com.lightbend.lagom.javadsl.persistence.*;
import com.lightbend.lagom.javadsl.persistence.cassandra.CassandraReadSide;
import com.lightbend.lagom.javadsl.persistence.cassandra.CassandraSession;
import org.oscm.lagom.enums.Messages;
import org.oscm.lagom.exceptions.InternalException;
import org.oscm.provisioning.impl.data.*;
import org.oscm.provisioning.impl.data.ReleaseCommand.*;
import org.oscm.rudder.api.RudderService;
import org.oscm.rudder.api.data.ReleaseStatusResponse;
import org.pcollections.PSequence;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.lightbend.lagom.javadsl.persistence.cassandra.CassandraReadSide.completedStatement;

@Singleton
public class ReleaseScheduler {

    private static final String REGEX_SERVICE = "v1/Service";
    private static final String REGEX_NEXT = "==>";
    private static final String REGEX_NONE = "<none>";
    private static final String REGEX_NODES = "<nodes>";
    private static final String REGEX_REPLACE = "<nodes>";

    private static final int SERVICE_COLUMNS = 5;
    private static final int SERVICE_COLUMN_NAME = 0;
    private static final int SERVICE_COLUMN_EXT_IP = 2;
    private static final int SERVICE_COLUMN_PORTS = 3;

    private static final String KEY_IP = "ip";
    private static final String KEY_PORT = "port";

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
            session.select("SELECT id, status FROM releaseSchedule WHERE tag IN ? AND status IN ?",
                ReleaseEvent.TAG.allTags(), statuses)
                .runForeach(row -> {
                    UUID id = row.getUUID("id");
                    ReleaseStatus status = row
                        .get("status", ReleaseStatus.class);

                    PersistentEntityRef<ReleaseCommand> ref = registry
                        .refFor(ReleaseEntity.class, id.toString());

                    ref.ask(InternalGetReleaseState.INSTANCE).thenCompose(
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
                                return installRelease(service, ref, release, state);
                            case UPDATING:
                                return updateRelease(service, ref, release, state);
                            case DELETING:
                                return deleteRelease(service, ref, instance);
                            case PENDING:
                                return commitRelease(service, ref, release, instance, target);
                            case DEPLOYED:
                                return checkRelease(service, ref, release, instance);
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

    private CompletionStage<Done> installRelease(RudderService service,
        PersistentEntityRef<ReleaseCommand> ref, Release release, ReleaseState state) {
        return service.install()
            .invoke(release.getAsInstallRequest(state.getInstance()))
            .thenCompose(notUsed -> ref.ask(InternalInitiateRelease.INSTANCE))
            .exceptionally(
                throwable -> {
                    if (throwable instanceof TransportException) {
                        TransportException te = (TransportException) throwable;

                        if (te.errorCode() == TransportErrorCode.InternalServerError) {
                            InternalException ie = new InternalException(
                                Messages.ERROR_BAD_RESPONSE, te); // TODO replace exception

                            ref.ask(new InternalFailRelease(ie.getAsFailure()));
                        }
                    }

                    return Done.getInstance();
                });
    }

    private CompletionStage<Done> updateRelease(RudderService service,
        PersistentEntityRef<ReleaseCommand> ref, Release release, ReleaseState state) {
        return service.update()
            .invoke(release.getAsUpdateRequest(state.getInstance()))
            .thenCompose(notUsed -> ref.ask(InternalInitiateRelease.INSTANCE))
            .exceptionally(
                throwable -> {
                    if (throwable instanceof TransportException) {
                        TransportException te = (TransportException) throwable;

                        if (te.errorCode() == TransportErrorCode.InternalServerError) {
                            InternalException ie = new InternalException(
                                Messages.ERROR_BAD_RESPONSE, te); // TODO replace exception

                            ref.ask(new InternalFailRelease(ie.getAsFailure()));
                        }
                    }

                    return Done.getInstance();
                });
    }

    private CompletionStage<Done> deleteRelease(RudderService service,
        PersistentEntityRef<ReleaseCommand> ref, String instance) {
        return service.delete(instance)
            .invoke().thenCompose(notUsed -> ref.ask(InternalInitiateRelease.INSTANCE))
            .exceptionally(
                throwable -> {
                    if (throwable instanceof TransportException) {
                        TransportException te = (TransportException) throwable;

                        if (te.errorCode() == TransportErrorCode.InternalServerError) {
                            InternalException ie = new InternalException(
                                Messages.ERROR_BAD_RESPONSE, te); // TODO replace exception

                            ref.ask(new InternalFailRelease(ie.getAsFailure()));
                        }
                    }

                    return Done.getInstance();
                });
    }

    private CompletionStage<Done> commitRelease(RudderService service,
        PersistentEntityRef<ReleaseCommand> ref, Release release, String instance, URI target) {
        return service.status(instance, release.getVersion())
            .invoke()
            .thenCompose(
                response -> {
                    Integer code = response.getInfo().getStatus().getCode();

                    switch (code) {
                    case ReleaseStatusResponse.UNKNOWN:
                        return CompletableFuture.completedFuture(Done.getInstance());
                    case ReleaseStatusResponse.DEPLOYED:
                        Map<String, String> services = extractServices(release.getEndpoints(),
                            response.getInfo().getStatus().getResources(), instance,
                            target.getHost());
                        return ref.ask(
                            new InternalConfirmRelease(services));
                    case ReleaseStatusResponse.DELETED:
                        return ref.ask(InternalDeleteRelease.INSTANCE);
                    default:
                        InternalException ie = new InternalException(
                            Messages.ERROR_BAD_RESPONSE); // TODO replace exception

                        return ref.ask(
                            new InternalFailRelease(ie.getAsFailure()));
                    }
                })
            .exceptionally(
                throwable -> {
                    if (throwable instanceof TransportException) {
                        TransportException te = (TransportException) throwable;

                        if (te.errorCode() == TransportErrorCode.InternalServerError) {
                            InternalException ie = new InternalException(
                                Messages.ERROR_BAD_RESPONSE, te); // TODO replace exception

                            ref.ask(new InternalFailRelease(ie.getAsFailure()));
                        }
                    }

                    return Done.getInstance();
                });
    }

    private CompletionStage<Done> checkRelease(RudderService service,
        PersistentEntityRef<ReleaseCommand> ref, Release release, String instance) {
        return service.status(instance, release.getVersion())
            .invoke()
            .thenCompose(
                response -> {
                    Integer code = response.getInfo().getStatus().getCode();

                    switch (code) {
                    case ReleaseStatusResponse.DEPLOYED:
                        return CompletableFuture.completedFuture(Done.getInstance());
                    case ReleaseStatusResponse.DELETED:
                        return ref.ask(InternalDeleteRelease.INSTANCE);
                    default:
                        InternalException ie = new InternalException(
                            Messages.ERROR_BAD_RESPONSE); // TODO replace exception
                        return ref.ask(new InternalFailRelease(ie.getAsFailure()));
                    }
                })
            .exceptionally(
                throwable -> {
                    if (throwable instanceof TransportException) {
                        TransportException te = (TransportException) throwable;

                        if (te.errorCode() == TransportErrorCode.InternalServerError) {
                            InternalException ie = new InternalException(
                                Messages.ERROR_BAD_RESPONSE, te); // TODO replace exception

                            ref.ask(new InternalFailRelease(ie.getAsFailure()));
                        }
                    }

                    return Done.getInstance();
                });
    }

    private Map<String, String> extractServices(Map<String, String> templates, String resources,
        String instance, String host) {

        if (templates == null) {
            return null;
        }

        int begin = resources.indexOf(REGEX_SERVICE);
        int end = resources.indexOf(REGEX_NEXT, begin);

        if (begin < 0) {
            return Collections.emptyMap();
        }

        if (end < 0) {
            end = resources.length() - 1;
        }

        String[] words = resources.substring(begin, end).split(" ");

        HashMap<String, String> replacements = new HashMap<>();

        for (int i = SERVICE_COLUMNS + 1; i < words.length; i += SERVICE_COLUMNS) {

            String name = words[i + SERVICE_COLUMN_NAME];
            String extIp = words[i + SERVICE_COLUMN_EXT_IP];
            String ports = words[i + SERVICE_COLUMN_PORTS];

            name = name.replace(instance, "");

            List<String> portList = Arrays.stream(ports.split(","))
                .filter(port -> port.contains(":"))
                .map(port -> port.split("[:/]")[1]).collect(Collectors.toList());

            if (extIp.matches(REGEX_NONE) || portList.isEmpty()) {
                continue;
            } else if (extIp.matches(REGEX_NODES)) {
                extIp = host;
            }

            replacements.put(name + ":" + KEY_IP, extIp);

            for (int j = 0; j < portList.size(); j++) {
                replacements.put(name + ":" + KEY_PORT + ":" + j, extIp);
            }
        }

        HashMap<String, String> endpoints = new HashMap<>(templates);

        Pattern pattern = Pattern.compile(REGEX_REPLACE);

        endpoints.replaceAll((key, value) -> {
            Matcher matcher = pattern.matcher(value);
            StringBuffer sb = new StringBuffer();

            while (matcher.find()) {
                String replacement = replacements.get(matcher.group(1));
                matcher.appendReplacement(sb, replacement != null ? replacement : "null");
            }
            matcher.appendTail(sb);

            return sb.toString();
        });

        return endpoints;
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
                    "tag varchar, " +
                    "status varchar, " +
                    "PRIMARY KEY (id)" +
                    ")")
                .thenCompose(d -> session.executeCreateTable(
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
