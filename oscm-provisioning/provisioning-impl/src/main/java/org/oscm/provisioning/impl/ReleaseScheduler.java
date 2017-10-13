/*
 * ****************************************************************************
 *                                                                                
 *    Copyright FUJITSU LIMITED 2017                                           
 *                                                                                                                                
 *    Creation Date: 2017-09-21              
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
import org.oscm.lagom.filters.BasicAuthFilter;
import org.oscm.provisioning.impl.data.*;
import org.oscm.provisioning.impl.data.ReleaseCommand.*;
import org.oscm.rudder.api.RudderService;
import org.oscm.rudder.api.data.ReleaseStatusResponse;
import org.pcollections.PSequence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.FiniteDuration;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.lightbend.lagom.javadsl.persistence.cassandra.CassandraReadSide.completedStatement;

/**
 * Scheduler to execute, confirm and monitor releases via rudder.
 * <p>
 * It uses a database table to collect qualifying entities that are sharded over the cluster via
 * their tags. The scheduler starts two watchdogs to handle releases. One for fast execution and one
 * for slow monitoring. State changes are propagated to the entities via commands.
 */
@Singleton
public class ReleaseScheduler {

    private static final String REGEX_SERVICE = "v1/Service";
    private static final String REGEX_NEXT = "==>";
    private static final String REGEX_NONE = "<none>";
    private static final String REGEX_NODES = "<nodes>";
    private static final String REGEX_REPLACE = "\\{(\\w+)\\}";

    private static final int SERVICE_COLUMNS = 5;
    private static final int SERVICE_COLUMN_NAME = 0;
    private static final int SERVICE_COLUMN_EXT_IP = 2;
    private static final int SERVICE_COLUMN_PORTS = 3;

    private static final String KEY_IP = "ip";
    private static final String KEY_PORT = "port";

    private static final Logger LOGGER = LoggerFactory.getLogger(ReleaseScheduler.class);

    private final ActorSystem system;
    private final RudderClientManager rudderClientManager;
    private final CassandraSession session;
    private final PersistentEntityRegistry registry;
    private final Materializer materializer;

    private final List<String> tags;

    @Inject
    public ReleaseScheduler(RudderClientManager rudderClientManager,
        CassandraSession session, ActorSystem system, ReadSide readSide,
        PersistentEntityRegistry registry, Materializer materializer) {

        this.system = system;
        this.rudderClientManager = rudderClientManager;
        this.session = session;
        this.registry = registry;
        this.materializer = materializer;

        tags = ReleaseEvent.TAG.allTags().stream().map(AggregateEventTag::tag).collect(
            Collectors.toList());

        FiniteDuration initialDelay = FiniteDuration
            .fromNanos(system.settings().config()
                .getDuration(Config.WATCHDOG_INITIAL_DELAY).toNanos());

        FiniteDuration executionInterval = FiniteDuration
            .fromNanos(system.settings().config()
                .getDuration(Config.WATCHDOG_EXECUTION_INTERVAL).toNanos());

        FiniteDuration monitorInterval = FiniteDuration
            .fromNanos(system.settings().config()
                .getDuration(Config.WATCHDOG_MONITOR_INTERVAL).toNanos());

        readSide.register(ReleaseProcessor.class);

        system.scheduler()
            .schedule(initialDelay, executionInterval, this::executeReleases, system.dispatcher());
        system.scheduler()
            .schedule(initialDelay, monitorInterval, this::monitorReleases, system.dispatcher());
    }

    /**
     * Watchdog task for fast execution and conformation of releases.
     */
    private void executeReleases() {
        serveRelease(Arrays.asList(ReleaseStatus.INSTALLING, ReleaseStatus.UPDATING,
            ReleaseStatus.DELETING, ReleaseStatus.PENDING));
    }

    /**
     * Watchdog task for slow monitoring of releases.
     */
    private void monitorReleases() {
        serveRelease(Collections.singletonList(ReleaseStatus.DEPLOYED));
    }

    /**
     * Common watchdog task. Checks the database for entries with the corresponding tags and
     * handles them according to their status.
     *
     * @param statuses the list of statuses to check for
     */
    private void serveRelease(List<ReleaseStatus> statuses) {
        try {
            session.select("SELECT id, status FROM releaseSchedule WHERE tag IN ?", tags)
                .runForeach(row -> {
                    UUID id = row.getUUID("id");
                    ReleaseStatus status = ReleaseStatus.valueOf(row.getString("status"));

                    if (!statuses.contains(status)) {
                        return;
                    }

                    PersistentEntityRef<ReleaseCommand> ref = registry
                        .refFor(ReleaseEntity.class, id.toString());

                    ref.ask(InternalGetReleaseState.INSTANCE).thenCompose(
                        state -> {
                            Release release = state.getRelease();
                            String instanceId = state.getInstanceId();

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
                                return deleteRelease(service, ref, instanceId);
                            case PENDING:
                                return commitRelease(service, ref, release, instanceId, target);
                            case DEPLOYED:
                                return checkRelease(service, ref, release, instanceId);
                            default:
                                return CompletableFuture.completedFuture(Done.getInstance());
                            }
                        });
                }, materializer).exceptionally(throwable -> {
                LOGGER.error("Unable to serve releases", throwable);
                return Done.getInstance();
            });
        } catch (IllegalStateException ise) {
            //ignore
        }
    }

    /**
     * Task to install the release via rudder.
     *
     * @param service the rudder service
     * @param ref     the entity reference
     * @param release the release info
     * @param state   the current entity state
     * @return eventually done
     */
    private CompletionStage<Done> installRelease(RudderService service,
        PersistentEntityRef<ReleaseCommand> ref, Release release, ReleaseState state) {

        String user = system.settings().config().getString(Config.RUDDER_USER);
        String password = system.settings().config().getString(Config.RUDDER_PASSWORD);

        return service.install()
            .handleRequestHeader(BasicAuthFilter.getFilter(user, password))
            .invoke(release.getAsInstallRequest(state.getInstanceId()))
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

    /**
     * Task to update the release via rudder.
     *
     * @param service the rudder service
     * @param ref     the entity reference
     * @param release the release info
     * @param state   the current entity state
     * @return eventually done
     */
    private CompletionStage<Done> updateRelease(RudderService service,
        PersistentEntityRef<ReleaseCommand> ref, Release release, ReleaseState state) {

        String user = system.settings().config().getString(Config.RUDDER_USER);
        String password = system.settings().config().getString(Config.RUDDER_PASSWORD);

        return service.update()
            .handleRequestHeader(BasicAuthFilter.getFilter(user, password))
            .invoke(release.getAsUpdateRequest(state.getInstanceId()))
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

    /**
     * Task to delete the release via rudder.
     *
     * @param service    the rudder service
     * @param ref        the entity reference
     * @param instanceId the instance id of the release
     * @return eventually done
     */
    private CompletionStage<Done> deleteRelease(RudderService service,
        PersistentEntityRef<ReleaseCommand> ref, String instanceId) {

        String user = system.settings().config().getString(Config.RUDDER_USER);
        String password = system.settings().config().getString(Config.RUDDER_PASSWORD);

        return service.delete(instanceId)
            .handleRequestHeader(BasicAuthFilter.getFilter(user, password))
            .invoke()
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

    /**
     * Task to check the release via rudder and to commit it if ready.
     *
     * @param service    the rudder service
     * @param ref        the entity reference
     * @param release    the release info
     * @param instanceId the instance id of the release
     * @param target     the URI of the target rudder proxy
     * @return eventually done
     */
    private CompletionStage<Done> commitRelease(RudderService service,
        PersistentEntityRef<ReleaseCommand> ref, Release release, String instanceId, URI target) {

        String user = system.settings().config().getString(Config.RUDDER_USER);
        String password = system.settings().config().getString(Config.RUDDER_PASSWORD);

        return service.status(instanceId, release.getVersion())
            .handleRequestHeader(BasicAuthFilter.getFilter(user, password))
            .invoke()
            .thenCompose(
                response -> {
                    Integer code = response.getInfo().getStatus().getCode();

                    switch (code) {
                    case ReleaseStatusResponse.Info.Status.UNKNOWN:
                        return CompletableFuture.completedFuture(Done.getInstance());
                    case ReleaseStatusResponse.Info.Status.DEPLOYED:
                        Map<String, String> endpoints = extractEndpoints(release.getEndpoints(),
                            response.getInfo().getStatus().getResources(), instanceId,
                            target.getHost());
                        return ref.ask(
                            new InternalConfirmRelease(endpoints));
                    case ReleaseStatusResponse.Info.Status.DELETED:
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

    /**
     * Task to check the release and report a failure if not ready.
     *
     * @param service    the rudder service
     * @param ref        the entity reference
     * @param release    the release info
     * @param instanceId the instance id of the release
     * @return eventually done
     */
    private CompletionStage<Done> checkRelease(RudderService service,
        PersistentEntityRef<ReleaseCommand> ref, Release release, String instanceId) {

        String user = system.settings().config().getString(Config.RUDDER_USER);
        String password = system.settings().config().getString(Config.RUDDER_PASSWORD);

        return service.status(instanceId, release.getVersion())
            .handleRequestHeader(BasicAuthFilter.getFilter(user, password))
            .invoke()
            .thenCompose(
                response -> {
                    Integer code = response.getInfo().getStatus().getCode();

                    switch (code) {
                    case ReleaseStatusResponse.Info.Status.DEPLOYED:
                        return CompletableFuture.completedFuture(Done.getInstance());
                    case ReleaseStatusResponse.Info.Status.DELETED:
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

    /**
     * Extracts the service endpoints from the given resource string and replaces the matches in the
     * given templates.
     *
     * @param templates  the endpoint template map
     * @param resources  the resource string of the release
     * @param instanceId the instance id of the release
     * @param host       the host of the target cluster
     * @return the replaced endpoint map
     */
    private Map<String, String> extractEndpoints(Map<String, String> templates, String resources,
        String instanceId, String host) {

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

            name = name.replace(instanceId, "");

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

    /**
     * Read side processor for release events.
     * <p>
     * Manages the entries in the scheduler database table according to events.
     */
    public static class ReleaseProcessor
        extends ReadSideProcessor<ReleaseEvent> {

        private static final String OFFSET_ID = "release-processor-offset";

        private final CassandraReadSide readSide;
        private final CassandraSession session;

        private PreparedStatement insertStatement;
        private PreparedStatement updateStatement;
        private PreparedStatement deleteStatement;

        @Inject
        public ReleaseProcessor(CassandraReadSide readSide, CassandraSession session) {
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

        /**
         * Creates the database table for the scheduler.
         *
         * @return eventually done
         */
        private CompletionStage<Done> createTable() {
            return session.executeCreateTable(
                "CREATE TABLE IF NOT EXISTS releaseSchedule ( " +
                    "tag varchar, " +
                    "id uuid, " +
                    "status varchar, " +
                    "PRIMARY KEY (tag, id)" +
                    ")"
            );
        }

        /**
         * Prepares the insert statement for the scheduler table.
         *
         * @return eventually done
         */
        private CompletionStage<Done> prepareInsertStatement() {
            return session.prepare("INSERT INTO releaseSchedule(id, tag, status) VALUES (?, ?, ?)")
                .thenApply(s -> {
                    insertStatement = s;
                    return Done.getInstance();
                });
        }

        /**
         * Prepares the update statement for the scheduler table.
         *
         * @return eventually done
         */
        private CompletionStage<Done> prepareUpdateStatement() {
            return session.prepare("UPDATE releaseSchedule SET status = ? WHERE id = ? AND tag = ?")
                .thenApply(s -> {
                    updateStatement = s;
                    return Done.getInstance();
                });
        }

        /**
         * Prepares the delete statement for the scheduler table.
         *
         * @return eventually done
         */
        private CompletionStage<Done> prepareDeleteStatement() {
            return session.prepare("DELETE FROM releaseSchedule WHERE id = ? AND tag = ?")
                .thenApply(s -> {
                    deleteStatement = s;
                    return Done.getInstance();
                });
        }

        /**
         * Inserts a new entry for the given event and status into the scheduler table.
         *
         * @param event  the release event
         * @param status the release status
         * @return eventually bound statement
         */
        private CompletionStage<List<BoundStatement>> insert(ReleaseEvent event,
            ReleaseStatus status) {

            return completedStatement(insertStatement.bind(event.getId(),
                ReleaseEvent.TAG.forEntityId(event.getIdString()).tag(), status.name()
            ));
        }

        /**
         * Updates the corresponding entry with the given event and status in the scheduler table.
         *
         * @param event  the release event
         * @param status the release status
         * @return eventually bound statement
         */
        private CompletionStage<List<BoundStatement>> update(ReleaseEvent event,
            ReleaseStatus status) {

            return completedStatement(updateStatement.bind(status.name(), event.getId(),
                ReleaseEvent.TAG.forEntityId(event.getIdString()).tag()
            ));
        }

        /**
         * Deletes the corresponding entry to the given event in the scheduler table.
         *
         * @param event the release event
         * @return eventually bound statement
         */
        private CompletionStage<List<BoundStatement>> delete(ReleaseEvent event) {
            return completedStatement(deleteStatement.bind(
                event.getId(), ReleaseEvent.TAG.forEntityId(event.getIdString()).tag()
            ));
        }

        @Override
        public PSequence<AggregateEventTag<ReleaseEvent>> aggregateTags() {
            return ReleaseEvent.TAG.allTags();
        }
    }

}
