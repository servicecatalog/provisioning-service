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
import org.oscm.provisioning.impl.data.ReleaseCommand;
import org.oscm.provisioning.impl.data.ReleaseEvent;
import org.oscm.provisioning.impl.data.ReleaseState;

import javax.inject.Inject;
import java.util.Optional;
import java.util.UUID;

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
            this::installingRelease);

        builder.setReadOnlyCommandHandler(ReleaseCommand.DeleteRelease.class,
            this::alreadyDone);

        builder.setEventHandlerChangingBehavior(
            ReleaseEvent.InstallingRelease.class,
            event -> installing(state().installing(event)));

        addGetReleaseHandler(builder);

        return builder.build();
    }

    private Behavior installing(ReleaseState state) {
        BehaviorBuilder builder = newBehaviorBuilder(state);

        builder.setCommandHandler(ReleaseCommand.UpdateRelease.class,
            this::installingRelease);

        builder.setCommandHandler(ReleaseCommand.DeleteRelease.class,
            this::deletedRelease);

        builder.setCommandHandler(ReleaseCommand.InternalCommitRelease.class,
            this::pendingRelease);

        builder.setCommandHandler(ReleaseCommand.InternalFailRelease.class,
            this::failedRelease);

        builder.setEventHandler(
            ReleaseEvent.InstallingRelease.class,
            state()::installing);

        builder.setEventHandlerChangingBehavior(
            ReleaseEvent.DeletedRelease.class,
            evt -> deleted(state().deleted(evt)));

        builder.setEventHandlerChangingBehavior(
            ReleaseEvent.PendingRelease.class,
            evt -> pending(state.pending(evt)));

        builder.setEventHandlerChangingBehavior(
            ReleaseEvent.FailedRelease.class,
            evt -> failed(state().failed(evt)));

        addGetReleaseHandler(builder);

        return builder.build();
    }

    private Behavior updating(ReleaseState state) {
        BehaviorBuilder builder = newBehaviorBuilder(state);

        builder.setCommandHandler(ReleaseCommand.UpdateRelease.class,
            this::updatingRelease);

        builder.setCommandHandler(ReleaseCommand.DeleteRelease.class,
            this::deletingRelease);

        builder.setCommandHandler(ReleaseCommand.InternalCommitRelease.class,
            this::pendingRelease);

        builder.setCommandHandler(ReleaseCommand.InternalFailRelease.class,
            this::errorRelease);

        builder.setEventHandler(ReleaseEvent.UpdatingRelease.class,
            state()::updating);

        builder.setEventHandlerChangingBehavior(
            ReleaseEvent.DeletingRelease.class,
            event -> deleting(state().deleting(event))
        );

        builder.setEventHandlerChangingBehavior(
            ReleaseEvent.PendingRelease.class,
            evt -> pending(state.pending(evt)));

        builder.setEventHandlerChangingBehavior(
            ReleaseEvent.ErrorRelease.class,
            event -> error(state().error(event))
        );

        addGetReleaseHandler(builder);

        return builder.build();
    }

    private Behavior deleting(ReleaseState state) {
        BehaviorBuilder builder = newBehaviorBuilder(state);

        builder.setReadOnlyCommandHandler(ReleaseCommand.UpdateRelease.class,
            this::alreadyDone);

        builder.setReadOnlyCommandHandler(ReleaseCommand.DeleteRelease.class,
            this::alreadyDone);

        addGetReleaseHandler(builder);

        return builder.build();
    }

    private Behavior pending(ReleaseState state) {
        BehaviorBuilder builder = newBehaviorBuilder(state);

        builder.setCommandHandler(ReleaseCommand.UpdateRelease.class,
            this::updatingRelease);

        builder.setCommandHandler(ReleaseCommand.DeleteRelease.class,
            this::deletingRelease);

        builder.setCommandHandler(ReleaseCommand.InternalCommitRelease.class,
            this::deployedRelease);

        builder.setCommandHandler(ReleaseCommand.InternalDeleteRelease.class,
            this::deletedRelease);

        builder.setCommandHandler(ReleaseCommand.InternalFailRelease.class,
            this::errorRelease);

        builder.setEventHandlerChangingBehavior(
            ReleaseEvent.UpdatingRelease.class,
            evt -> updating(state().updating(evt))
        );

        builder.setEventHandlerChangingBehavior(
            ReleaseEvent.DeletingRelease.class,
            evt -> deleting(state().deleting(evt))
        );

        builder.setEventHandlerChangingBehavior(
            ReleaseEvent.DeployedRelease.class,
            evt -> deployed(state().deployed(evt))
        );

        builder.setEventHandlerChangingBehavior(
            ReleaseEvent.DeletedRelease.class,
            evt -> deleted(state().deleted(evt))
        );

        builder.setEventHandlerChangingBehavior(
            ReleaseEvent.ErrorRelease.class,
            evt -> error(state().error(evt))
        );

        return builder.build();
    }

    private Behavior deployed(ReleaseState state) {
        BehaviorBuilder builder = newBehaviorBuilder(state);

        builder.setCommandHandler(ReleaseCommand.UpdateRelease.class,
            this::updatingRelease);

        builder.setCommandHandler(ReleaseCommand.DeleteRelease.class,
            this::deletingRelease);

        builder.setCommandHandler(ReleaseCommand.InternalDeleteRelease.class,
            this::deletedRelease);

        builder.setCommandHandler(ReleaseCommand.InternalFailRelease.class,
            this::errorRelease);

        builder.setEventHandlerChangingBehavior(
            ReleaseEvent.UpdatingRelease.class,
            event -> updating(state().updating(event)));

        builder.setEventHandlerChangingBehavior(
            ReleaseEvent.DeletingRelease.class,
            event -> deleting(state().deleting(event))
        );

        builder.setEventHandlerChangingBehavior(
            ReleaseEvent.DeletedRelease.class,
            evt -> deleted(state().deleted(evt))
        );

        builder.setEventHandlerChangingBehavior(
            ReleaseEvent.ErrorRelease.class,
            evt -> error(state().error(evt))
        );

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
            this::installingRelease);

        builder.setReadOnlyCommandHandler(ReleaseCommand.DeleteRelease.class,
            this::alreadyDone);

        builder.setEventHandlerChangingBehavior(
            ReleaseEvent.InstallingRelease.class,
            evt -> installing(state().installing(evt)));

        addGetReleaseHandler(builder);

        return builder.build();
    }

    private Behavior error(ReleaseState state) {
        BehaviorBuilder builder = newBehaviorBuilder(state);

        builder.setCommandHandler(ReleaseCommand.UpdateRelease.class,
            this::updatingRelease);

        builder.setCommandHandler(ReleaseCommand.DeleteRelease.class,
            this::deletingRelease);

        builder.setEventHandlerChangingBehavior(
            ReleaseEvent.UpdatingRelease.class,
            evt -> updating(state().updating(evt)));

        builder.setEventHandlerChangingBehavior(
            ReleaseEvent.DeletingRelease.class,
            evt -> deleting(state().deleting(evt))
        );

        builder.setEventHandler(
            ReleaseEvent.ErrorRelease.class,
            state()::error
        );

        addGetReleaseHandler(builder);

        return builder.build();
    }

    private Persist<ReleaseEvent> installingRelease(
        ReleaseCommand.UpdateRelease cmd, CommandContext<Done> ctx) {
        return ctx.thenPersist(
            new ReleaseEvent.InstallingRelease(UUID.fromString(entityId()),
                System.currentTimeMillis(), INSTANCE_PREFIX + entityId(),
                cmd.getRelease()),
            evt -> ctx.reply(Done.getInstance()));
    }

    private Persist<ReleaseEvent> updatingRelease(
        ReleaseCommand.UpdateRelease cmd, CommandContext<Done> ctx) {
        return ctx.thenPersist(
            new ReleaseEvent.UpdatingRelease(UUID.fromString(entityId()),
                System.currentTimeMillis(), cmd.getRelease()),
            evt -> ctx.reply(Done.getInstance()));
    }

    private Persist<ReleaseEvent> deletingRelease(
        ReleaseCommand.DeleteRelease cmd, CommandContext<Done> ctx) {
        return ctx.thenPersist(
            new ReleaseEvent.DeletingRelease(UUID.fromString(entityId()),
                System.currentTimeMillis()),
            evt -> ctx.reply(Done.getInstance()));
    }

    private Persist<ReleaseEvent> pendingRelease(
        ReleaseCommand.InternalCommitRelease cmd,
        CommandContext<Done> ctx) {
        return ctx.thenPersist(
            new ReleaseEvent.PendingRelease(UUID.fromString(entityId()),
                System.currentTimeMillis()),
            evt -> ctx.reply(Done.getInstance()));
    }

    private Persist<ReleaseEvent> deployedRelease(
        ReleaseCommand.InternalCommitRelease cmd,
        CommandContext<Done> ctx) {
        return ctx.thenPersist(
            new ReleaseEvent.DeployedRelease(UUID.fromString(entityId()),
                System.currentTimeMillis(), cmd.getServices()),
            evt -> ctx.reply(Done.getInstance()));
    }

    private Persist<ReleaseEvent> deletedRelease(
        ReleaseCommand cmd,
        CommandContext<Done> ctx) {
        return ctx.thenPersist(
            new ReleaseEvent.DeletedRelease(UUID.fromString(entityId()),
                System.currentTimeMillis()),
            evt -> ctx.reply(Done.getInstance()));
    }

    private Persist<ReleaseEvent> failedRelease(
        ReleaseCommand.InternalFailRelease cmd, CommandContext<Done> ctx) {
        return ctx.thenPersist(
            new ReleaseEvent.FailedRelease(UUID.fromString(entityId()),
                System.currentTimeMillis(), cmd.getFailure()),
            evt -> ctx.reply(Done.getInstance()));
    }

    private Persist<ReleaseEvent> errorRelease(
        ReleaseCommand.InternalFailRelease cmd, CommandContext<Done> ctx) {
        return ctx.thenPersist(
            new ReleaseEvent.ErrorRelease(UUID.fromString(entityId()),
                System.currentTimeMillis(), cmd.getFailure()),
            evt -> ctx.reply(Done.getInstance()));
    }

    private void addGetReleaseHandler(BehaviorBuilder builder) {
        builder.setReadOnlyCommandHandler(ReleaseCommand.GetRelease.class,
            (cmd, ctx) -> ctx.reply(state().getAsAPI()));

        builder.setReadOnlyCommandHandler(
            ReleaseCommand.InternalGetReleaseState.class,
            (cmd, ctx) -> ctx.reply(state()));
    }

    private void alreadyDone(Object cmd, ReadOnlyCommandContext<Done> ctx) {
        ctx.reply(Done.getInstance());
    }
}
