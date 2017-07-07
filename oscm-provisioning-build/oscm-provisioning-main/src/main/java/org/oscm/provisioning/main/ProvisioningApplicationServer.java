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

import org.oscm.common.interfaces.data.Version;
import org.oscm.common.interfaces.keys.ConfigurationKey;
import org.oscm.common.kafka.EntityTable;
import org.oscm.common.kafka.KafkaConfig;
import org.oscm.common.kafka.Stream;
import org.oscm.common.kafka.TimedStream;
import org.oscm.common.kafka.TransitionStream;
import org.oscm.common.util.ApplicationServer;
import org.oscm.common.util.ConfigurationManager;
import org.oscm.common.util.ServiceManager;
import org.oscm.provisioning.interfaces.data.Release;
import org.oscm.provisioning.interfaces.data.Subscription;
import org.oscm.provisioning.interfaces.enums.Activity;
import org.oscm.provisioning.interfaces.enums.Application;
import org.oscm.provisioning.interfaces.enums.Config;
import org.oscm.provisioning.interfaces.enums.Entity;
import org.oscm.provisioning.interfaces.enums.Transition;
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
        keys.addAll(Arrays.asList(Config.values()));

        ConfigurationManager.init(importer, Application.SELF, Activity.values(),
                Config.LATEST_VERSION, new Version(1, 0, 0),
                keys.toArray(new ConfigurationKey[] {}));

        // Initialize kafka streams
        EntityTable<Subscription> subscriptionTable = new EntityTable<>(
                Entity.SUBSCRIPTION);
        EntityTable<Release> releaseTable = new EntityTable<>(Entity.RELEASE);

        TransitionStream provisionStream = new TransitionStream(
                Transition.PROVISION);
        TimedStream updateStream = new TimedStream(Transition.UPDATE, 10000); // ms
        TimedStream monitorStream = new TimedStream(Transition.MONITOR, 600000); // ms

        streams = new ArrayList<>();
        streams.add(subscriptionTable);
        streams.add(releaseTable);
        streams.add(provisionStream);
        streams.add(updateStream);
        streams.add(monitorStream);

        // register services and their supplier in the service manager
        ServiceManager sm = ServiceManager.getInstance();

        // Event classes
        sm.setEventSource(Entity.SUBSCRIPTION, subscriptionTable);
        sm.setEventSource(Entity.RELEASE, releaseTable);

        // Service classes
        sm.setTransitionService(Transition.PROVISION,
                new ProvisionService()::provision);
        sm.setTransitionService(Transition.UPDATE, new UpdateService()::update);
        sm.setTransitionService(Transition.MONITOR,
                new UpdateService()::monitor);

        // startup streams
        streams.forEach((s) -> s.start());
    }

    @Override
    protected void stop() {
        streams.forEach((s) -> s.stop());
    }

}
