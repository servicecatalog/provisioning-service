/*
 * ****************************************************************************
 *
 *    Copyright FUJITSU LIMITED 2017
 *
 *    Creation Date: 2017-08-02
 *
 * ****************************************************************************
 */

package org.oscm.provisioning.impl;

import akka.Done;
import akka.actor.ActorSystem;
import akka.actor.Cancellable;
import com.lightbend.lagom.javadsl.persistence.PersistentEntity;
import com.lightbend.lagom.javadsl.persistence.PersistentEntityRegistry;
import org.oscm.lagom.enums.Messages;
import org.oscm.lagom.exceptions.ConnectionException;
import org.oscm.lagom.exceptions.ValidationException;
import org.oscm.lagom.filters.BasicAuthFilter;
import org.oscm.provisioning.api.data.ProvisioningRelease;
import org.oscm.provisioning.impl.data.*;
import org.oscm.rudder.api.RudderService;
import org.oscm.rudder.api.data.ReleaseStatusResponse;
import scala.concurrent.duration.Duration;

import javax.inject.Inject;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class ReleaseEntity extends
    PersistentEntity<ReleaseCommand, ReleaseEvent, ReleaseState> {

    private static final String INSTANCE_PREFIX = "oscm-";

    private final ActorSystem system;
    private final PersistentEntityRegistry registry;
    private final RudderClientManager clientManager;

    private volatile Cancellable watchdog;

    @Inject
    public ReleaseEntity(ActorSystem system, PersistentEntityRegistry registry,
        RudderClientManager clientManager) {
        this.system = system;
        this.registry = registry;
        this.clientManager = clientManager;
    }

    @Override
    public Behavior initialBehavior(Optional<ReleaseState> snapshot) {

        if (!snapshot.isPresent()) {
            return none(ReleaseState.none());
        } else {
            ReleaseState state = snapshot.get();
            switch (state.getStatus()) {
            case NONE:
                return none(state);
            case INSTALLING:
                return installing(state);
            case UPDATING:
                return updating(state);
            case DELETING:
                return deleting(state);
            case DEPLOYED:
                return deployed(state);
            case DELETED:
                return deleted(state);
            case FAILED:
                return failed(state);
            case ERROR:
                return error(state);
            default:
                throw new IllegalStateException();
            }
        }
    }

    private Behavior none(ReleaseState state) {
        BehaviorBuilder builder = newBehaviorBuilder(state);

        builder.setCommandHandler(ReleaseCommand.UpdateRelease.class,
            this::installRelease);

        builder.setReadOnlyCommandHandler(ReleaseCommand.DeleteRelease.class,
            this::alreadyDone);

        builder.setEventHandlerChangingBehavior(
            ReleaseEvent.InstallingRelease.class,
            event -> installing(state().installing(event)));

        builder.setEventHandlerChangingBehavior(
            ReleaseEvent.FailedRelease.class,
            event -> failed(state().failed(event)));

        addGetReleaseHandler(builder);

        return builder.build();
    }

    private Behavior installing(ReleaseState state) {
        BehaviorBuilder builder = newBehaviorBuilder(state);

        builder.setCommandHandler(ReleaseCommand.UpdateRelease.class,
            this::updateRelease);

        builder.setCommandHandler(ReleaseCommand.DeleteRelease.class,
            this::deleteRelease);

        builder.setEventHandlerChangingBehavior(
            ReleaseEvent.UpdatingRelease.class,
            event -> updating(state().updating(event)));

        builder.setEventHandlerChangingBehavior(
            ReleaseEvent.DeletingRelease.class,
            event -> deleting(state().deleting(event))
        );

        addWatchdogHandler(builder);

        startWatchdog(system.settings().config()
            .getDuration(Config.WATCHDOG_STATUS_INTERVAL));

        addGetReleaseHandler(builder);

        return builder.build();
    }

    private Behavior updating(ReleaseState state) {
        BehaviorBuilder builder = newBehaviorBuilder(state);

        builder.setCommandHandler(ReleaseCommand.UpdateRelease.class,
            this::updateRelease);

        builder.setCommandHandler(ReleaseCommand.DeleteRelease.class,
            this::deleteRelease);

        builder.setEventHandler(ReleaseEvent.UpdatingRelease.class,
            event -> state().updating(event));

        builder.setEventHandlerChangingBehavior(
            ReleaseEvent.DeletingRelease.class,
            event -> deleting(state().deleting(event))
        );

        addWatchdogHandler(builder);

        startWatchdog(system.settings().config()
            .getDuration(Config.WATCHDOG_STATUS_INTERVAL));

        addGetReleaseHandler(builder);

        return builder.build();
    }

    private Behavior deleting(ReleaseState state) {
        BehaviorBuilder builder = newBehaviorBuilder(state);

        builder.setReadOnlyCommandHandler(ReleaseCommand.UpdateRelease.class,
            this::alreadyDone);

        builder.setReadOnlyCommandHandler(ReleaseCommand.DeleteRelease.class,
            this::alreadyDone);

        addWatchdogHandler(builder);

        startWatchdog(system.settings().config()
            .getDuration(Config.WATCHDOG_STATUS_INTERVAL));

        addGetReleaseHandler(builder);

        return builder.build();
    }

    private Behavior deployed(ReleaseState state) {
        BehaviorBuilder builder = newBehaviorBuilder(state);

        builder.setCommandHandler(ReleaseCommand.UpdateRelease.class,
            this::updateRelease);

        builder.setCommandHandler(ReleaseCommand.DeleteRelease.class,
            this::deleteRelease);

        builder.setEventHandlerChangingBehavior(
            ReleaseEvent.UpdatingRelease.class,
            event -> updating(state().updating(event)));

        builder.setEventHandlerChangingBehavior(
            ReleaseEvent.DeletingRelease.class,
            event -> deleting(state().deleting(event))
        );

        addWatchdogHandler(builder);

        startWatchdog(system.settings().config()
            .getDuration(Config.WATCHDOG_MONITOR_INTERVAL));

        addGetReleaseHandler(builder);

        return builder.build();
    }

    private Behavior deleted(ReleaseState state) {
        BehaviorBuilder builder = newBehaviorBuilder(state);

        builder.setReadOnlyCommandHandler(ReleaseCommand.UpdateRelease.class,
            this::alreadyDone);

        builder.setReadOnlyCommandHandler(ReleaseCommand.DeleteRelease.class,
            this::alreadyDone);

        addGetReleaseHandler(builder);

        return builder.build();
    }

    private Behavior failed(ReleaseState state) {
        BehaviorBuilder builder = newBehaviorBuilder(state);

        builder.setCommandHandler(ReleaseCommand.UpdateRelease.class,
            this::installRelease);

        builder.setReadOnlyCommandHandler(ReleaseCommand.DeleteRelease.class,
            this::alreadyDone);

        builder.setEventHandlerChangingBehavior(
            ReleaseEvent.InstallingRelease.class,
            event -> installing(state().installing(event)));

        builder.setEventHandler(
            ReleaseEvent.ErrorRelease.class,
            state()::error
        );

        addGetReleaseHandler(builder);

        return builder.build();
    }

    private Behavior error(ReleaseState state) {
        BehaviorBuilder builder = newBehaviorBuilder(state);

        builder.setCommandHandler(ReleaseCommand.UpdateRelease.class,
            this::updateRelease);

        builder.setCommandHandler(ReleaseCommand.DeleteRelease.class,
            this::deleteRelease);

        builder.setEventHandlerChangingBehavior(
            ReleaseEvent.UpdatingRelease.class,
            event -> updating(state().updating(event)));

        builder.setEventHandlerChangingBehavior(
            ReleaseEvent.DeletingRelease.class,
            event -> deleting(state().deleting(event))
        );

        builder.setEventHandler(
            ReleaseEvent.ErrorRelease.class,
            state()::error
        );

        addGetReleaseHandler(builder);

        return builder.build();
    }

    private Persist<ReleaseEvent> installRelease(
        ReleaseCommand.UpdateRelease cmd,
        CommandContext<Done> ctx) {

        Release release = cmd.getRelease();

        RudderService service;
        try {
            service = clientManager
                .getServiceForURI(new URI(release.getTarget()));
        } catch (NullPointerException | URISyntaxException e) {
            return ctx.thenPersist(
                new ReleaseEvent.FailedRelease(UUID.fromString(entityId()),
                    System.currentTimeMillis(), release,
                    new ValidationException(
                        Messages.ERROR_INVALID_URL,
                        Release.FIELD_TARGET).getAsFailure()),
                event -> ctx.reply(Done.getInstance()));
        }

        String instance = INSTANCE_PREFIX + entityId();

        String user = system.settings().config().getString(Config.RUDDER_USER);
        String password = system.settings().config()
            .getString(Config.RUDDER_PASSWORD);

        try {
            return service.install()
                .handleRequestHeader(BasicAuthFilter.getFilter(user, password))
                .invoke(release.getAsInstallRequest(instance))
                .<Persist<ReleaseEvent>>thenApply(
                    notUsed -> ctx.thenPersist(
                        new ReleaseEvent.InstallingRelease(
                            UUID.fromString(entityId()),
                            System.currentTimeMillis(),
                            instance, release),
                        event -> ctx.reply(Done.getInstance())))
                .exceptionally(
                    throwable -> ctx.thenPersist(
                        new ReleaseEvent.FailedRelease(
                            UUID.fromString(entityId()),
                            System.currentTimeMillis(),
                            release,
                            new ConnectionException(
                                Messages.ERROR_CONNECTION_FAILURE, throwable)
                                .getAsFailure()),
                        event -> ctx.reply(Done.getInstance())))
                .toCompletableFuture().get();
        } catch (InterruptedException | ExecutionException e) {
            //TODO Log error
            return ctx.done();
        }
    }

    private Persist<ReleaseEvent> updateRelease(
        ReleaseCommand.UpdateRelease cmd,
        CommandContext<Done> ctx) {

        Release release = cmd.getRelease();

        RudderService service;
        try {
            service = clientManager
                .getServiceForURI(new URI(release.getTarget()));
        } catch (NullPointerException | URISyntaxException e) {
            return ctx.thenPersist(
                new ReleaseEvent.ErrorRelease(UUID.fromString(entityId()),
                    System.currentTimeMillis(),
                    new ValidationException(
                        Messages.ERROR_INVALID_URL,
                        Release.FIELD_TARGET).getAsFailure()),
                event -> ctx.reply(Done.getInstance()));
        }

        String user = system.settings().config().getString(Config.RUDDER_USER);
        String password = system.settings().config()
            .getString(Config.RUDDER_PASSWORD);

        try {
            return service.update()
                .handleRequestHeader(BasicAuthFilter.getFilter(user, password))
                .invoke(release.getAsUpdateRequest(state().getInstance()))
                .<Persist<ReleaseEvent>>thenApply(
                    notUsed -> ctx.thenPersist(
                        new ReleaseEvent.UpdatingRelease(
                            UUID.fromString(entityId()),
                            System.currentTimeMillis(),
                            release),
                        event -> ctx.reply(Done.getInstance())))
                .exceptionally(
                    throwable -> ctx.thenPersist(
                        new ReleaseEvent.ErrorRelease(
                            UUID.fromString(entityId()),
                            System.currentTimeMillis(),
                            new ConnectionException(
                                Messages.ERROR_CONNECTION_FAILURE, throwable)
                                .getAsFailure()),
                        event -> ctx.reply(Done.getInstance())))
                .toCompletableFuture().get();
        } catch (InterruptedException | ExecutionException e) {
            //TODO Log error
            return ctx.done();
        }
    }

    private Persist<ReleaseEvent> deleteRelease(
        ReleaseCommand.DeleteRelease cmd,
        CommandContext<Done> ctx) {

        RudderService service;
        try {
            service = clientManager
                .getServiceForURI(new URI(state().getRelease().getTarget()));
        } catch (NullPointerException | URISyntaxException e) {
            return ctx.thenPersist(
                new ReleaseEvent.ErrorRelease(UUID.fromString(entityId()),
                    System.currentTimeMillis(),
                    new ValidationException(
                        Messages.ERROR_INVALID_URL,
                        Release.FIELD_TARGET).getAsFailure()),
                event -> ctx.reply(Done.getInstance()));
        }

        String user = system.settings().config().getString(Config.RUDDER_USER);
        String password = system.settings().config()
            .getString(Config.RUDDER_PASSWORD);

        try {
            return service.delete(state().getInstance())
                .handleRequestHeader(BasicAuthFilter.getFilter(user, password))
                .invoke()
                .<Persist<ReleaseEvent>>thenApply(
                    notUsed -> ctx.thenPersist(
                        new ReleaseEvent.DeletingRelease(
                            UUID.fromString(entityId()),
                            System.currentTimeMillis()),
                        event -> ctx.reply(Done.getInstance())))
                .exceptionally(
                    throwable -> ctx.thenPersist(
                        new ReleaseEvent.ErrorRelease(
                            UUID.fromString(entityId()),
                            System.currentTimeMillis(),
                            new ConnectionException(
                                Messages.ERROR_CONNECTION_FAILURE, throwable)
                                .getAsFailure()),
                        event -> ctx.reply(Done.getInstance())))
                .toCompletableFuture().get();
        } catch (InterruptedException | ExecutionException e) {
            //TODO Log error
            return ctx.done();
        }
    }

    private void startWatchdog(java.time.Duration interval) {

        if (watchdog != null) {
            watchdog.cancel();
        }

        watchdog = system.scheduler()
            .schedule(Duration.create(0, TimeUnit.SECONDS),
                Duration.fromNanos(interval.toNanos()), this::updateStatus,
                system.dispatcher());
    }

    private void updateStatus() {
        RudderService service;
        try {
            service = clientManager
                .getServiceForURI(new URI(state().getRelease().getTarget()));
        } catch (NullPointerException | URISyntaxException e) {
            throw new RuntimeException("Invalid URL");
        }

        String user = system.settings().config().getString(Config.RUDDER_USER);
        String password = system.settings().config()
            .getString(Config.RUDDER_PASSWORD);

        try {
            ReleaseStatusResponse response = service
                .status(state().getInstance(),
                    state().getRelease().getVersion())
                .handleRequestHeader(BasicAuthFilter.getFilter(user, password))
                .invoke()
                .exceptionally(throwable -> {
                    //TODO Log error
                    return null;
                }).toCompletableFuture().get();

            if (response == null) {
                return;
            }

            switch (response.getInfo().getStatus().getCode()) {
            case ReleaseStatusResponse.UNKNOWN:
            case ReleaseStatusResponse.SUPERSEDED:
            case ReleaseStatusResponse.FAILED:
                registry.refFor(getClass(), entityId())
                    .ask(new ReleaseCommand.InternalErrorRelease(null))
                    .toCompletableFuture().get();
                break;
            case ReleaseStatusResponse.DEPLOYED:
                Map<String, String> services = extractServices(
                    response.getInfo().getStatus().getResources());
                registry.refFor(getClass(), entityId())
                    .ask(new ReleaseCommand.InternalDeployedRelease(services))
                    .toCompletableFuture().get();
                break;
            case ReleaseStatusResponse.DELETED:
                registry.refFor(getClass(), entityId())
                    .ask(ReleaseCommand.InternalDeletedRelease.INSTANCE)
                    .toCompletableFuture().get();
                break;
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Execution interrupted or aborted");
        }
    }

    private Map<String, String> extractServices(String resources) {
        return null;
    }

    private void addWatchdogHandler(BehaviorBuilder builder) {

        builder.setCommandHandler(ReleaseCommand.InternalDeployedRelease.class,
            (cmd, ctx) -> ctx.thenPersist(
                new ReleaseEvent.DeployedRelease(UUID.fromString(entityId()),
                    System.currentTimeMillis(), cmd.getServices()),
                event -> ctx.reply(Done.getInstance())));

        builder.setCommandHandler(ReleaseCommand.InternalDeletedRelease.class,
            (cmd, ctx) -> ctx.thenPersist(
                new ReleaseEvent.DeletedRelease(UUID.fromString(entityId()),
                    System.currentTimeMillis()),
                event -> ctx.reply(Done.getInstance())));

        builder.setCommandHandler(ReleaseCommand.InternalErrorRelease.class,
            (cmd, ctx) -> ctx.thenPersist(
                new ReleaseEvent.ErrorRelease(UUID.fromString(entityId()),
                    System.currentTimeMillis(), cmd.getFailure()),
                event -> ctx.reply(Done.getInstance())));

        builder.setEventHandlerChangingBehavior(
            ReleaseEvent.DeployedRelease.class,
            event -> deployed(state().deployed(event))
        );

        builder.setEventHandlerChangingBehavior(
            ReleaseEvent.DeletedRelease.class,
            event -> deleted(state().deleted(event))
        );

        builder.setEventHandlerChangingBehavior(
            ReleaseEvent.ErrorRelease.class,
            event -> error(state().error(event))
        );
    }

    private void addGetReleaseHandler(BehaviorBuilder builder) {
        builder.setReadOnlyCommandHandler(ReleaseCommand.GetRelease.class,
            this::getRelease);
    }

    private void getRelease(ReleaseCommand.GetRelease cmd,
        ReadOnlyCommandContext<ProvisioningRelease> ctx) {
        ctx.reply(state().getAsAPI());
    }

    private void alreadyDone(Object cmd, ReadOnlyCommandContext<Done> ctx) {
        ctx.reply(Done.getInstance());
    }
}
