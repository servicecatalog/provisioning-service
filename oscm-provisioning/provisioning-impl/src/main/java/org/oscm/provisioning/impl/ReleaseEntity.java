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
import com.lightbend.lagom.javadsl.persistence.PersistentEntity;
import org.oscm.provisioning.impl.data.ReleaseCommand;
import org.oscm.provisioning.impl.data.ReleaseEvent;
import org.oscm.provisioning.impl.data.ReleaseState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.UUID;

/**
 * Entity class for releases.
 * <p>
 * Manages the state machine for the release process within each entity.
 */
public class ReleaseEntity extends
    PersistentEntity<ReleaseCommand, ReleaseEvent, ReleaseState> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReleaseEntity.class);

    private static final String INSTANCE_PREFIX = "oscm-";

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

    /**
     * Initial behaviour.
     *
     * @param state the current state
     * @return the behaviour
     */
    private Behavior none(ReleaseState state) {
        BehaviorBuilder builder = newBehaviorBuilder(state);

        builder.setCommandHandler(ReleaseCommand.UpdateRelease.class,
            this::installingRelease);

        builder.setReadOnlyCommandHandler(ReleaseCommand.DeleteRelease.class,
            this::ignore);

        builder.setReadOnlyCommandHandler(ReleaseCommand.InternalInitiateRelease.class,
            this::ignore);

        builder.setReadOnlyCommandHandler(ReleaseCommand.InternalConfirmRelease.class,
            this::ignore);

        builder.setReadOnlyCommandHandler(ReleaseCommand.InternalDeleteRelease.class,
            this::ignore);

        builder.setReadOnlyCommandHandler(ReleaseCommand.InternalFailRelease.class,
            this::ignore);

        builder.setEventHandlerChangingBehavior(
            ReleaseEvent.InstallingRelease.class,
            event -> installing(state().installing(event)));

        addGetReleaseHandler(builder);

        return builder.build();
    }

    /**
     * Behaviour during installation initialization.
     *
     * @param state the current state
     * @return the behaviour
     */
    private Behavior installing(ReleaseState state) {
        BehaviorBuilder builder = newBehaviorBuilder(state);

        builder.setCommandHandler(ReleaseCommand.UpdateRelease.class,
            this::installingRelease);

        builder.setCommandHandler(ReleaseCommand.DeleteRelease.class,
            this::deletedRelease);

        builder.setCommandHandler(ReleaseCommand.InternalInitiateRelease.class,
            this::pendingRelease);

        builder.setReadOnlyCommandHandler(ReleaseCommand.InternalConfirmRelease.class,
            this::ignore);

        builder.setReadOnlyCommandHandler(ReleaseCommand.InternalDeleteRelease.class,
            this::ignore);

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

    /**
     * Behaviour during update initialization.
     *
     * @param state the current state
     * @return the behaviour
     */
    private Behavior updating(ReleaseState state) {
        BehaviorBuilder builder = newBehaviorBuilder(state);

        builder.setCommandHandler(ReleaseCommand.UpdateRelease.class,
            this::updatingRelease);

        builder.setCommandHandler(ReleaseCommand.DeleteRelease.class,
            this::deletingRelease);

        builder.setCommandHandler(ReleaseCommand.InternalInitiateRelease.class,
            this::pendingRelease);

        builder.setReadOnlyCommandHandler(ReleaseCommand.InternalConfirmRelease.class,
            this::ignore);

        builder.setReadOnlyCommandHandler(ReleaseCommand.InternalDeleteRelease.class,
            this::ignore);

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

    /**
     * Behaviour during deletion initialization.
     *
     * @param state the current state
     * @return the behaviour
     */
    private Behavior deleting(ReleaseState state) {
        BehaviorBuilder builder = newBehaviorBuilder(state);

        builder.setReadOnlyCommandHandler(ReleaseCommand.UpdateRelease.class,
            this::ignore);

        builder.setReadOnlyCommandHandler(ReleaseCommand.DeleteRelease.class,
            this::ignore);

        builder.setCommandHandler(ReleaseCommand.InternalInitiateRelease.class,
            this::pendingRelease);

        builder.setReadOnlyCommandHandler(ReleaseCommand.InternalConfirmRelease.class,
            this::ignore);

        builder.setReadOnlyCommandHandler(ReleaseCommand.InternalDeleteRelease.class,
            this::ignore);

        builder.setCommandHandler(ReleaseCommand.InternalFailRelease.class,
            this::errorRelease);

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

    /**
     * Behaviour during pending release state.
     *
     * @param state the current state
     * @return the behaviour
     */
    private Behavior pending(ReleaseState state) {
        BehaviorBuilder builder = newBehaviorBuilder(state);

        builder.setCommandHandler(ReleaseCommand.UpdateRelease.class,
            this::updatingRelease);

        builder.setCommandHandler(ReleaseCommand.DeleteRelease.class,
            this::deletingRelease);

        builder.setReadOnlyCommandHandler(ReleaseCommand.InternalInitiateRelease.class,
            this::ignore);

        builder.setCommandHandler(ReleaseCommand.InternalConfirmRelease.class,
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

        addGetReleaseHandler(builder);

        return builder.build();
    }

    /**
     * Behaviour after a confirmed deployment.
     *
     * @param state the current state
     * @return the behaviour
     */
    private Behavior deployed(ReleaseState state) {
        BehaviorBuilder builder = newBehaviorBuilder(state);

        builder.setCommandHandler(ReleaseCommand.UpdateRelease.class,
            this::updatingRelease);

        builder.setCommandHandler(ReleaseCommand.DeleteRelease.class,
            this::deletingRelease);

        builder.setReadOnlyCommandHandler(ReleaseCommand.InternalInitiateRelease.class,
            this::ignore);

        builder.setReadOnlyCommandHandler(ReleaseCommand.InternalConfirmRelease.class,
            this::ignore);

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

    /**
     * Behaviour after a confirmed deletion.
     *
     * @param state the current state
     * @return the behaviour
     */
    private Behavior deleted(ReleaseState state) {
        BehaviorBuilder builder = newBehaviorBuilder(state);

        builder.setReadOnlyCommandHandler(ReleaseCommand.UpdateRelease.class,
            this::ignore);

        builder.setReadOnlyCommandHandler(ReleaseCommand.DeleteRelease.class,
            this::ignore);

        builder.setReadOnlyCommandHandler(ReleaseCommand.InternalInitiateRelease.class,
            this::ignore);

        builder.setReadOnlyCommandHandler(ReleaseCommand.InternalConfirmRelease.class,
            this::ignore);

        builder.setReadOnlyCommandHandler(ReleaseCommand.InternalDeleteRelease.class,
            this::ignore);

        builder.setReadOnlyCommandHandler(ReleaseCommand.InternalFailRelease.class,
            this::ignore);

        addGetReleaseHandler(builder);

        return builder.build();
    }

    /**
     * Behaviour after a failure on an installation attempt.
     *
     * @param state the current state
     * @return the behaviour
     */
    private Behavior failed(ReleaseState state) {
        BehaviorBuilder builder = newBehaviorBuilder(state);

        builder.setCommandHandler(ReleaseCommand.UpdateRelease.class,
            this::installingRelease);

        builder.setReadOnlyCommandHandler(ReleaseCommand.DeleteRelease.class,
            this::ignore);

        builder.setReadOnlyCommandHandler(ReleaseCommand.InternalInitiateRelease.class,
            this::ignore);

        builder.setReadOnlyCommandHandler(ReleaseCommand.InternalConfirmRelease.class,
            this::ignore);

        builder.setReadOnlyCommandHandler(ReleaseCommand.InternalDeleteRelease.class,
            this::ignore);

        builder.setReadOnlyCommandHandler(ReleaseCommand.InternalFailRelease.class,
            this::ignore);

        builder.setEventHandlerChangingBehavior(
            ReleaseEvent.InstallingRelease.class,
            evt -> installing(state().installing(evt)));

        addGetReleaseHandler(builder);

        return builder.build();
    }

    /**
     * Behaviour after a failure on an active release.
     *
     * @param state the current state
     * @return the behaviour
     */
    private Behavior error(ReleaseState state) {
        BehaviorBuilder builder = newBehaviorBuilder(state);

        builder.setCommandHandler(ReleaseCommand.UpdateRelease.class,
            this::updatingRelease);

        builder.setCommandHandler(ReleaseCommand.DeleteRelease.class,
            this::deletingRelease);

        builder.setReadOnlyCommandHandler(ReleaseCommand.InternalInitiateRelease.class,
            this::ignore);

        builder.setReadOnlyCommandHandler(ReleaseCommand.InternalConfirmRelease.class,
            this::ignore);

        builder.setReadOnlyCommandHandler(ReleaseCommand.InternalDeleteRelease.class,
            this::ignore);

        builder.setReadOnlyCommandHandler(ReleaseCommand.InternalFailRelease.class,
            this::ignore);

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

    /**
     * Command handler for installing releases.
     *
     * @param cmd the command
     * @param ctx the context
     * @return a InstallingRelease event
     */
    private Persist<ReleaseEvent> installingRelease(
        ReleaseCommand.UpdateRelease cmd, CommandContext<Done> ctx) {
        LOGGER.info("Install release with id {}", entityId());
        return ctx.thenPersist(
            new ReleaseEvent.InstallingRelease(UUID.fromString(entityId()),
                System.currentTimeMillis(), INSTANCE_PREFIX + entityId(),
                cmd.getRelease()),
            evt -> ctx.reply(Done.getInstance()));
    }

    /**
     * Command handler for updating releases.
     *
     * @param cmd the command
     * @param ctx the context
     * @return a UpdatingRelease event
     */
    private Persist<ReleaseEvent> updatingRelease(
        ReleaseCommand.UpdateRelease cmd, CommandContext<Done> ctx) {
        LOGGER.info("Update release with id {}", entityId());
        return ctx.thenPersist(
            new ReleaseEvent.UpdatingRelease(UUID.fromString(entityId()),
                System.currentTimeMillis(), cmd.getRelease()),
            evt -> ctx.reply(Done.getInstance()));
    }

    /**
     * Command handler for deleting releases.
     *
     * @param cmd the command
     * @param ctx the context
     * @return a DeletingRelease event
     */
    private Persist<ReleaseEvent> deletingRelease(
        ReleaseCommand.DeleteRelease cmd, CommandContext<Done> ctx) {
        LOGGER.info("Delete release with id {}", entityId());
        return ctx.thenPersist(
            new ReleaseEvent.DeletingRelease(UUID.fromString(entityId()),
                System.currentTimeMillis()),
            evt -> ctx.reply(Done.getInstance()));
    }

    /**
     * Command handler for pending releases.
     *
     * @param cmd the command
     * @param ctx the context
     * @return a PendingRelease event
     */
    private Persist<ReleaseEvent> pendingRelease(
        ReleaseCommand.InternalInitiateRelease cmd, CommandContext<Done> ctx) {
        LOGGER.info("Pending release with id {}", entityId());
        return ctx.thenPersist(
            new ReleaseEvent.PendingRelease(UUID.fromString(entityId()),
                System.currentTimeMillis()),
            evt -> ctx.reply(Done.getInstance()));
    }

    /**
     * Command handler for deployed releases.
     *
     * @param cmd the command
     * @param ctx the context
     * @return a DeployedRelease event
     */
    private Persist<ReleaseEvent> deployedRelease(
        ReleaseCommand.InternalConfirmRelease cmd, CommandContext<Done> ctx) {
        LOGGER.info("Deployed release with id {}", entityId());
        return ctx.thenPersist(
            new ReleaseEvent.DeployedRelease(UUID.fromString(entityId()),
                System.currentTimeMillis(), cmd.getEndpoints()),
            evt -> ctx.reply(Done.getInstance()));
    }

    /**
     * Command handler for deleted releases.
     *
     * @param cmd the command
     * @param ctx the context
     * @return a DeletedRelease event
     */
    private Persist<ReleaseEvent> deletedRelease(
        ReleaseCommand cmd, CommandContext<Done> ctx) {
        LOGGER.info("Deleted release with id {}", entityId());
        return ctx.thenPersist(
            new ReleaseEvent.DeletedRelease(UUID.fromString(entityId()),
                System.currentTimeMillis()),
            evt -> ctx.reply(Done.getInstance()));
    }

    /**
     * Command handler for failed new releases.
     *
     * @param cmd the command
     * @param ctx the context
     * @return a FailedRelease event
     */
    private Persist<ReleaseEvent> failedRelease(
        ReleaseCommand.InternalFailRelease cmd, CommandContext<Done> ctx) {
        LOGGER.info("Failed release with id {}", entityId());
        return ctx.thenPersist(
            new ReleaseEvent.FailedRelease(UUID.fromString(entityId()),
                System.currentTimeMillis(), cmd.getFailure()),
            evt -> ctx.reply(Done.getInstance()));
    }

    /**
     * Command handler for failed active releases.
     *
     * @param cmd the command
     * @param ctx the context
     * @return a ErrorRelease event
     */
    private Persist<ReleaseEvent> errorRelease(
        ReleaseCommand.InternalFailRelease cmd, CommandContext<Done> ctx) {
        LOGGER.info("Error release with id {}", entityId());
        return ctx.thenPersist(
            new ReleaseEvent.ErrorRelease(UUID.fromString(entityId()),
                System.currentTimeMillis(), cmd.getFailure()),
            evt -> ctx.reply(Done.getInstance()));
    }

    /**
     * Adds basic behaviour to the given builder.
     *
     * @param builder the behaviour builder
     */
    private void addGetReleaseHandler(BehaviorBuilder builder) {
        builder.setReadOnlyCommandHandler(ReleaseCommand.GetRelease.class,
            (cmd, ctx) -> ctx.reply(state().getAsAPI()));

        builder.setReadOnlyCommandHandler(
            ReleaseCommand.InternalGetReleaseState.class,
            (cmd, ctx) -> ctx.reply(state()));
    }

    /**
     * Command handler for ignored commands.
     *
     * @param cmd the command
     * @param ctx the context
     */
    private void ignore(Object cmd, ReadOnlyCommandContext<Done> ctx) {
        ctx.reply(Done.getInstance());
    }
}
