/*******************************************************************************
 *                                                                              
 *  Copyright FUJITSU LIMITED 2017                                           
 *                                                                                                                                 
 *  Creation Date: Jun 27, 2017                                                      
 *                                                                              
 *******************************************************************************/

package org.oscm.provisioning.main;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.oscm.common.interfaces.keys.ConfigurationKey;
import org.oscm.common.kafka.CommandProducer;
import org.oscm.common.kafka.CommandStream;
import org.oscm.common.kafka.EventTable;
import org.oscm.common.kafka.KafkaConfig;
import org.oscm.common.kafka.Stream;
import org.oscm.common.rest.JerseyConfig;
import org.oscm.common.rest.JerseyResourceConfig;
import org.oscm.common.util.ApplicationServer;
import org.oscm.common.util.ConfigurationManager;
import org.oscm.common.util.ServiceManager;
import org.oscm.provisioning.interfaces.data.Sample;
import org.oscm.provisioning.interfaces.enums.Activity;
import org.oscm.provisioning.interfaces.enums.Application;
import org.oscm.provisioning.interfaces.enums.Entity;
import org.oscm.provisioning.interfaces.enums.Version;
import org.oscm.provisioning.services.SampleService;

/**
 * Startup class to orchestrate the application and its technologies.
 */
public class SampleApplicationServer extends ApplicationServer {

    private static final String BASE_URI = "http://localhost:%s/%s";

    /**
     * See {@link Application#flow(Application, String...)} for applicable
     * parameters
     */
    public static void main(String[] args) throws Exception {

        SampleApplicationServer app = new SampleApplicationServer();

        app.flow(args);
    }

    private HttpServer server;
    private List<Stream> streams;

    @Override
    protected void start() throws Exception {

        // combine necessary configuration keys and initialize configuration
        // manager
        List<ConfigurationKey> keys = new ArrayList<>();
        keys.addAll(Arrays.asList(KafkaConfig.values()));
        keys.addAll(Arrays.asList(JerseyConfig.values()));

        ConfigurationManager.init(importer, Application.SELF, Activity.values(),
                Version.values(), Version.LATEST, Version.V_1_0_0,
                keys.toArray(new ConfigurationKey[] {}));

        ConfigurationManager cm = ConfigurationManager.getInstance();

        // Initialize kafka streams
        CommandProducer cmdProducer = new CommandProducer(Application.SELF);
        EventTable<Sample> sampleTable = new EventTable<>(Entity.SAMPLE);
        CommandStream sampleCreateStream = new CommandStream(
                Activity.SAMPLE_CREATE);
        CommandStream sampleUpdateStream = new CommandStream(
                Activity.SAMPLE_UPDATE);
        CommandStream sampleDeleteStream = new CommandStream(
                Activity.SAMPLE_DELETE);

        streams = new ArrayList<>();
        streams.add(cmdProducer);
        streams.add(sampleTable);
        streams.add(sampleCreateStream);
        streams.add(sampleUpdateStream);
        streams.add(sampleDeleteStream);

        // register services and their supplier in the service manager
        ServiceManager sm = ServiceManager.getInstance();

        // Event classes
        sm.setPublisher(Application.SELF, cmdProducer);
        sm.setEventSource(Entity.SAMPLE, sampleTable);

        // Service classes
        sm.setCommandService(Activity.SAMPLE_CREATE,
                new SampleService()::create);
        sm.setCommandService(Activity.SAMPLE_UPDATE,
                new SampleService()::update);
        sm.setCommandService(Activity.SAMPLE_DELETE,
                new SampleService()::delete);
        sm.setQueryService(Activity.SAMPLE_READ, new SampleService()::read);
        sm.setQueryService(Activity.SAMPLE_READ_BY_NAME,
                new SampleService()::readByName);
        sm.setQueryService(Activity.SAMPLE_READ_ALL,
                new SampleService()::readAll);

        // startup streams
        streams.forEach((s) -> s.start());

        // startup rest api
        String port = cm.getConfig(JerseyConfig.JERSEY_PORT);
        String context = cm.getConfig(JerseyConfig.JERSEY_CONTEXT);
        String uri = String.format(BASE_URI, port, context);

        // String keystoreLoc = sc //TODO uncomment after testing phase
        // .getConfig(JerseyConfig.JERSEY_KEYSTORE_LOCATION);
        // String keystorePwd = sc
        // .getConfig(JerseyConfig.JERSEY_KEYSTORE_PASSWORD);

        // final SslConfigurator sslConfig = SslConfigurator.newInstance()
        // .keyStoreFile(keystoreLoc).keyStorePassword(keystorePwd);

        server = GrizzlyHttpServerFactory.createHttpServer(URI.create(uri),
                new JerseyResourceConfig());// , sslConfig.createSSLContext(),
                                            // true);
    }

    @Override
    protected void stop() {
        server.shutdown();

        streams.forEach((s) -> s.stop());
    }

}
