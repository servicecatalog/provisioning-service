/*
 * ****************************************************************************
 *
 *    Copyright FUJITSU LIMITED 2017
 *
 *    Creation Date: 2017-08-01
 *
 * ****************************************************************************
 */

package org.oscm.provisioning.impl;

import akka.Done;
import akka.japi.Pair;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Source;
import com.lightbend.lagom.javadsl.api.broker.Topic;
import com.lightbend.lagom.javadsl.broker.TopicProducer;
import com.lightbend.lagom.javadsl.persistence.AggregateEventTag;
import com.lightbend.lagom.javadsl.persistence.Offset;
import com.lightbend.lagom.javadsl.persistence.PersistentEntityRef;
import com.lightbend.lagom.javadsl.persistence.PersistentEntityRegistry;
import org.oscm.core.api.CoreService;
import org.oscm.core.api.data.CoreSubscription;
import org.oscm.provisioning.api.ProvisioningService;
import org.oscm.provisioning.api.data.ProvisioningRelease;
import org.oscm.provisioning.impl.data.Release;
import org.oscm.provisioning.impl.data.ReleaseCommand;
import org.oscm.provisioning.impl.data.ReleaseEvent;

import javax.inject.Inject;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class ProvisioningServiceImpl implements ProvisioningService {

    private final PersistentEntityRegistry registry;

    @Inject
    public ProvisioningServiceImpl(
        PersistentEntityRegistry registry,
        CoreService coreService) {
        this.registry = registry;

        registry.register(ReleaseEntity.class);

        coreService.subscriptionTopic().subscribe().atLeastOnce(
            Flow.<CoreSubscription>create()
                .mapAsync(1, this::consumeSubscription));
    }

    private CompletionStage<Done> consumeSubscription(
        CoreSubscription subscription) {

        if (subscription == null || subscription.getId() == null) {
            return CompletableFuture.completedFuture(Done.getInstance());
        }

        PersistentEntityRef<ReleaseCommand> ref = registry
            .refFor(ReleaseEntity.class, subscription.getIdString());

        CompletionStage<Done> stage = CompletableFuture
            .completedFuture(Done.getInstance());

        if (subscription.getOperation()
            == CoreSubscription.Operation.UPDATE) {
            stage = ref.ask(new ReleaseCommand.UpdateRelease(
                new Release(subscription)));

        } else if (subscription.getOperation()
            == CoreSubscription.Operation.DELETE) {
            stage = ref.ask(ReleaseCommand.DeleteRelease.INSTANCE);
        }

        stage.exceptionally(throwable -> {
            //TODO Log error
            return Done.getInstance();
        });

        return stage;
    }

    @Override
    public Topic<ProvisioningRelease> releaseTopic() {
        return TopicProducer
            .taggedStreamWithOffset(ReleaseEvent.TAG.allTags(),
                this::streamRelease);
    }

    private Source<Pair<ProvisioningRelease, Offset>, ?> streamRelease(
        AggregateEventTag<ReleaseEvent> tag, Offset offset) {
        return registry.eventStream(tag, offset)
            .mapAsync(1, pair -> {

                String id = pair.first().getIdString();

                return registry.refFor(ReleaseEntity.class, id)
                    .ask(ReleaseCommand.GetRelease.INSTANCE)
                    .thenApply(release -> Pair.create(release, pair.second()));
            });
    }
}
