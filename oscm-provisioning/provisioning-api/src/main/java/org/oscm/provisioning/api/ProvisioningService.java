/*
 * ****************************************************************************
 *
 *    Copyright FUJITSU LIMITED 2017
 *
 *    Creation Date: 2017-08-01
 *
 * ****************************************************************************
 */

package org.oscm.provisioning.api;

import com.lightbend.lagom.javadsl.api.Descriptor;
import com.lightbend.lagom.javadsl.api.Service;
import com.lightbend.lagom.javadsl.api.broker.Topic;
import com.lightbend.lagom.javadsl.api.broker.kafka.KafkaProperties;
import org.oscm.provisioning.api.data.ProvisioningRelease;

import static com.lightbend.lagom.javadsl.api.Service.named;

public interface ProvisioningService extends Service {

    String SERVICE_NAME = "provisioning";
    String TOPIC_RELEASE = "provisioning-release";

    Topic<ProvisioningRelease> releaseTopic();

    @Override
    default Descriptor descriptor() {
        return named(SERVICE_NAME).withTopics(
            Service.topic(TOPIC_RELEASE, this::releaseTopic)
                .withProperty(KafkaProperties.partitionKeyStrategy(),
                    ProvisioningRelease::getIdString)
        );
    }
}
