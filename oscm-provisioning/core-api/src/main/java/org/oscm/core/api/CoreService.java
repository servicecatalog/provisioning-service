/*
 * ****************************************************************************
 *
 *    Copyright FUJITSU LIMITED 2017
 *
 *    Creation Date: 2017-08-02
 *
 * ****************************************************************************
 */

package org.oscm.core.api;

import com.lightbend.lagom.javadsl.api.Descriptor;
import com.lightbend.lagom.javadsl.api.Service;
import com.lightbend.lagom.javadsl.api.broker.Topic;
import com.lightbend.lagom.javadsl.api.broker.kafka.KafkaProperties;
import org.oscm.core.api.data.CoreSubscription;

import static com.lightbend.lagom.javadsl.api.Service.named;
import static com.lightbend.lagom.javadsl.api.Service.topic;

/**
 * Service interface describing OSCM core topics used by the provisioning service.
 */
public interface CoreService extends Service {

    String SERVICE_NAME = "core";
    String TOPIC_SUBSCRIPTION = "core-subscription";

    /**
     * Kafka topic for subscriptions.
     *
     * @return the topic
     */
    Topic<CoreSubscription> subscriptionTopic();

    @Override
    default Descriptor descriptor() {
        return named(SERVICE_NAME).withTopics(
            topic(TOPIC_SUBSCRIPTION, this::subscriptionTopic)
                .withProperty(
                    KafkaProperties.partitionKeyStrategy(),
                    CoreSubscription::getIdString)
        );
    }
}
