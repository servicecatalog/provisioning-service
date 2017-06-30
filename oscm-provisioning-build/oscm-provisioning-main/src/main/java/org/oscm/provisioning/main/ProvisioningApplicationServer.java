/*******************************************************************************
 *                                                                              
 *  Copyright FUJITSU LIMITED 2017                                           
 *                                                                                                                                 
 *  Creation Date: Jun 27, 2017                                                      
 *                                                                              
 *******************************************************************************/

package org.oscm.provisioning.main;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.oscm.common.interfaces.keys.ConfigurationKey;
import org.oscm.common.kafka.EventStream;
import org.oscm.common.kafka.EventTable;
import org.oscm.common.kafka.KafkaConfig;
import org.oscm.common.kafka.Stream;
import org.oscm.common.util.ApplicationServer;
import org.oscm.common.util.ConfigurationManager;
import org.oscm.common.util.ServiceManager;
import org.oscm.provisioning.interfaces.data.Release;
import org.oscm.provisioning.interfaces.data.Subscription;
import org.oscm.provisioning.interfaces.enums.Activity;
import org.oscm.provisioning.interfaces.enums.Application;
import org.oscm.provisioning.interfaces.enums.Entity;
import org.oscm.provisioning.interfaces.enums.Transition;
import org.oscm.provisioning.interfaces.enums.Version;
import org.oscm.provisioning.services.ProvisionService;
import org.oscm.provisioning.services.UpdateService;

/**
 * Startup class to orchestrate the application and its technologies.
 */
public class ProvisioningApplicationServer extends ApplicationServer {

    /**
     * See {@link Application#flow(Application, String...)} for applicable
     * parameters
     */
    public static void main(String[] args) throws Exception {

        ProvisioningApplicationServer app = new ProvisioningApplicationServer();

        app.flow(args);
    }

    private List<Stream> streams;

    @Override
    protected void start() throws Exception {

        // combine necessary configuration keys and initialize configuration
        // manager
        List<ConfigurationKey> keys = new ArrayList<>();
        keys.addAll(Arrays.asList(KafkaConfig.values()));

        ConfigurationManager.init(importer, Application.SELF, Activity.values(),
                Version.values(), Version.LATEST, Version.V_1_0_0,
                keys.toArray(new ConfigurationKey[] {}));

        // Initialize kafka streams
        EventTable<Subscription> subscriptionTable = new EventTable<>(
                Entity.SUBSCRIPTION);
        EventTable<Release> releaseTable = new EventTable<>(Entity.RELEASE);

        EventStream provisionStream = new EventStream(Transition.PROVISION);
        EventStream updateStream = new EventStream(Transition.UPDATE);

        streams = new ArrayList<>();
        streams.add(subscriptionTable);
        streams.add(releaseTable);
        streams.add(provisionStream);
        streams.add(updateStream);

        // register services and their supplier in the service manager
        ServiceManager sm = ServiceManager.getInstance();

        // Event classes
        sm.setEventSource(Entity.SUBSCRIPTION, subscriptionTable);
        sm.setEventSource(Entity.RELEASE, releaseTable);

        // Service classes
        sm.setTransitionService(Transition.PROVISION,
                new ProvisionService()::provision);
        sm.setTransitionService(Transition.UPDATE, new UpdateService()::update);

        // startup streams
        streams.forEach((s) -> s.start());
    }

    @Override
    protected void stop() {
        streams.forEach((s) -> s.stop());
    }

}
