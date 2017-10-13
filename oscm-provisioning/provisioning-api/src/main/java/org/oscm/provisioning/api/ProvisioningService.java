/*
 * ****************************************************************************
 *
 *    Copyright FUJITSU LIMITED 2017
 *
 *    Creation Date: 2017-09-29
 *
 * ****************************************************************************
 */

package org.oscm.provisioning.api;

import akka.NotUsed;
import com.lightbend.lagom.javadsl.api.Descriptor;
import com.lightbend.lagom.javadsl.api.Service;
import com.lightbend.lagom.javadsl.api.ServiceCall;
import com.lightbend.lagom.javadsl.api.broker.Topic;
import com.lightbend.lagom.javadsl.api.broker.kafka.KafkaProperties;
import com.lightbend.lagom.javadsl.api.transport.Method;
import org.oscm.provisioning.api.data.ProvisioningRelease;

import static com.lightbend.lagom.javadsl.api.Service.named;
import static com.lightbend.lagom.javadsl.api.Service.restCall;

/**
 * Lagom interface for the provisioning service.
 */
public interface ProvisioningService extends Service {

    String SERVICE_NAME = "provisioning";
    String TOPIC_RELEASE = "provisioning-release";

    /**
     * Endpoint for health checks.
     *
     * @return the service call
     */
    ServiceCall<NotUsed, String> health();

    /**
     * Kafka topic for releases.
     *
     * @return the topic
     */
    Topic<ProvisioningRelease> releaseTopic();

    @Override
    default Descriptor descriptor() {
        return named(SERVICE_NAME)
            .withCalls(
                restCall(Method.GET, "/health", this::health))
            .withTopics(
                Service.topic(TOPIC_RELEASE, this::releaseTopic)
                    .withProperty(KafkaProperties.partitionKeyStrategy(),
                        ProvisioningRelease::getIdString));
    }
}
