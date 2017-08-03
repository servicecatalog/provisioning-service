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
import com.lightbend.lagom.javadsl.persistence.PersistentEntity;
import org.oscm.provisioning.api.data.ProvisioningRelease;
import org.oscm.provisioning.impl.data.ReleaseCommand;
import org.oscm.provisioning.impl.data.ReleaseEvent;
import org.oscm.provisioning.impl.data.ReleaseState;

import java.util.Optional;

public class ReleaseEntity extends
    PersistentEntity<ReleaseCommand, ReleaseEvent, ReleaseState> {

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
            case DELETING:
            case DEPLOYED:
            case FAILED_INSTALL:
                return failedInstall(state);
            case FAILED_UPD_DEL:
            case DELETED:
                return null;
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
            ReleaseEvent.InstallingRelease.class, event -> {
                return installing(state().installing(event));
            });

        builder.setEventHandlerChangingBehavior(
            ReleaseEvent.FailedReleaseInstall.class, event -> {
                return failedInstall(state().failedInstall(event));
            });

        addGetRelease(builder);

        return builder.build();
    }

    private Behavior installing(ReleaseState state) {
        BehaviorBuilder builder = newBehaviorBuilder(state);

        builder.setCommandHandler(ReleaseCommand.UpdateRelease.class,
            this::updateRelease);

        builder.setCommandHandler(ReleaseCommand.DeleteRelease.class,
            this::deleteRelease);

        builder.setEventHandlerChangingBehavior(
            ReleaseEvent.UpdatingRelease.class, evt -> {
                return updating(ReleaseState.updating(evt.getRelease()));
            });

        builder.setEventHandlerChangingBehavior(
            ReleaseEvent.UpdatingRelease.class, evt -> {
                return updating(ReleaseState.updating(evt.getRelease()));
            });

        addGetRelease(builder);

        return builder.build();
    }

    private Behavior updating(ReleaseState state) {
        BehaviorBuilder builder = newBehaviorBuilder(state);

        builder.setCommandHandler(ReleaseCommand.UpdateRelease.class,
            this::updateRelease);

        builder.setCommandHandler(ReleaseCommand.DeleteRelease.class,
            this::deleteRelease);

        builder.setEventHandlerChangingBehavior(
            ReleaseEvent.InstallingRelease.class, evt -> {
                return installing(ReleaseState.installing(evt.getRelease()));
            });

        addGetRelease(builder);

        return builder.build();
    }

    private Behavior deleting(ReleaseState state) {
        return null;
    }

    private Behavior deployed(ReleaseState state) {
        return null;
    }

    private Behavior deleted(ReleaseState state) {
        return null;
    }

    private Behavior failedInstall(ReleaseState state) {
        return null;
    }

    private Behavior failedUpdateOrDelete(ReleaseState state) {
        return null;
    }

    private Persist<ReleaseEvent.UpdatingRelease> installRelease(
        ReleaseCommand.UpdateRelease cmd,
        CommandContext<Done> ctx) {
        return null;
    }

    private Persist<ReleaseEvent.UpdatingRelease> updateRelease(
        ReleaseCommand.UpdateRelease cmd,
        CommandContext<Done> ctx) {
        return null;
    }

    private Persist<ReleaseEvent.DeletingRelease> deleteRelease(
        ReleaseCommand.DeleteRelease cmd,
        CommandContext<Done> ctx) {

        return null;
    }

    private ReleaseState updateStatus(ReleaseEvent event) {

        return null;
    }

    private ReleaseState monitorStatus(ReleaseEvent event) {

        return null;
    }

    private void addGetRelease(BehaviorBuilder builder) {
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
