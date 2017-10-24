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
import akka.NotUsed;
import akka.japi.Pair;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Source;
import com.lightbend.lagom.javadsl.api.ServiceCall;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Implementation of the provisioning service.
 * <p>
 * The service subscribes to the subscription topic of the core service and executes an update or
 * delete command for each received subscription on the corresponding release entity. With each
 * resulting event, the current state of the release entity is written to the release topic.
 */
public class ProvisioningServiceImpl implements ProvisioningService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProvisioningServiceImpl.class);

    private final PersistentEntityRegistry registry;

    @Inject
    public ProvisioningServiceImpl(
        PersistentEntityRegistry registry,
        CoreService coreService) {
        this.registry = registry;

        registry.register(ReleaseEntity.class);

        coreService.subscriptionTopic().subscribe().atLeastOnce(
            Flow.<CoreSubscription>create().mapAsync(1, this::consumeSubscription));
    }

    /**
     * Consumes a single subscription received from the core service. Corresponding to the operation
     * it sends a command to the release entity.
     *
     * @param subscription the received subscription
     * @return eventually done
     */
    private CompletionStage<Done> consumeSubscription(
        CoreSubscription subscription) {

        try {
            if (subscription == null || subscription.getId() == null) {
                LOGGER.warn("Received invalid subscription");
                return CompletableFuture.completedFuture(Done.getInstance());
            }

            LOGGER.info("Received subscription with id {} and timestamp {}", subscription.getId(),
                subscription.getTimestamp());

            PersistentEntityRef<ReleaseCommand> ref = registry
                .refFor(ReleaseEntity.class, subscription.getIdString());

            CompletionStage<Done> stage = CompletableFuture
                .completedFuture(Done.getInstance());

            if (subscription.getOperation()
                == CoreSubscription.Operation.UPDATE) {
                LOGGER.info("Update release for subscription with id {}", subscription.getId());
                stage = ref.ask(new ReleaseCommand.UpdateRelease(
                    new Release(subscription)));

            } else if (subscription.getOperation()
                == CoreSubscription.Operation.DELETE) {
                LOGGER.info("Delete release for subscription with id {}", subscription.getId());
                stage = ref.ask(ReleaseCommand.DeleteRelease.INSTANCE);
            }

            stage.exceptionally(throwable -> {
                LOGGER.error(
                    String.format("Unable to consume subscription with id %s and timestamp %s",
                        subscription.getId(), subscription.getTimestamp()), throwable);
                return Done.getInstance();
            });

            return stage;
        } catch (Exception e) {
            LOGGER.error(String.format("Unable to consume subscription with id %s and timestamp %s",
                subscription.getId(), subscription.getTimestamp()), e);
            return CompletableFuture.completedFuture(Done.getInstance());
        }
    }

    @Override
    public ServiceCall<NotUsed, String> health() {
        return req -> CompletableFuture.completedFuture("ok");
    }

    @Override
    public Topic<ProvisioningRelease> releaseTopic() {
        return TopicProducer
            .taggedStreamWithOffset(ReleaseEvent.TAG.allTags(),
                this::streamRelease);
    }

    /**
     * Streams for every new release event the current state of its entity to the release topic of
     * the provisioning service.
     *
     * @param tag    the entity tag
     * @param offset the stream offset
     * @return the stream
     */
    private Source<Pair<ProvisioningRelease, Offset>, ?> streamRelease(
        AggregateEventTag<ReleaseEvent> tag, Offset offset) {
        return registry.eventStream(tag, offset)
            .mapAsync(1, pair -> {

                String id = pair.first().getIdString();
                LOGGER.info("Pre production with id {}", id);

                return registry.refFor(ReleaseEntity.class, id)
                    .ask(ReleaseCommand.GetRelease.INSTANCE)
                    .thenApply(release -> {
                        LOGGER.info("Produced release with id {} and timestamp {}",
                            release.getId(), release.getTimestamp());
                        return Pair.create(release, pair.second());
                    });
            });
    }
}
