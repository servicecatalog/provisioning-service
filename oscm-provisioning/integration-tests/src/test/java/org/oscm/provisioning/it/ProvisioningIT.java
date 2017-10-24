/*
 * ****************************************************************************
 *
 *    Copyright FUJITSU LIMITED 2017
 *
 *    Creation Date: 2017-10-13
 *
 * ****************************************************************************
 */

package org.oscm.provisioning.it;

import akka.NotUsed;
import akka.stream.javadsl.Source;
import akka.stream.testkit.TestSubscriber;
import akka.stream.testkit.javadsl.TestSink;
import com.lightbend.lagom.javadsl.api.ServiceCall;
import com.lightbend.lagom.javadsl.api.broker.Topic;
import com.lightbend.lagom.javadsl.testkit.ProducerStub;
import com.lightbend.lagom.javadsl.testkit.ProducerStubFactory;
import com.lightbend.lagom.javadsl.testkit.ServiceTest;
import com.lightbend.lagom.javadsl.testkit.ServiceTest.TestServer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.oscm.core.api.CoreService;
import org.oscm.core.api.data.CoreSubscription;
import org.oscm.core.api.data.CoreSubscription.Operation;
import org.oscm.core.api.data.CoreSubscription.Template;
import org.oscm.provisioning.api.ProvisioningService;
import org.oscm.provisioning.api.data.ProvisioningRelease;
import org.oscm.rudder.api.RudderService;
import org.oscm.rudder.api.data.InstallReleaseRequest;
import org.oscm.rudder.api.data.ReleaseStatusResponse;
import org.oscm.rudder.api.data.ReleaseStatusResponse.Info;
import org.oscm.rudder.api.data.ReleaseStatusResponse.Info.Status;
import org.oscm.rudder.api.data.UpdateReleaseRequest;
import play.inject.Bindings;
import scala.concurrent.duration.FiniteDuration;

import javax.inject.Inject;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ProvisioningIT {

    private static ProducerStub<CoreSubscription> coreProducer;
    private static TestServer server;

    @BeforeClass
    public static void setup() {
        server = ServiceTest
            .startServer(ServiceTest.defaultSetup().configureBuilder(b ->
                b.overrides(
                    Bindings.bind(CoreService.class).to(CoreServiceStub.class),
                    Bindings.bind(RudderService.class).to(RudderServiceStub.class)))
                .withCassandra(true));
    }

    @Test
    public void test() {
        CoreSubscription sub = new CoreSubscription(UUID.randomUUID(), 0L, Operation.UPDATE,
            "http://www.target.de", "namespace", new Template("repo", "name", "version"), null,
            null, null);

        coreProducer.send(sub);

        ProvisioningService provisioningService = server.client(ProvisioningService.class);

        Source<ProvisioningRelease, ?> releaseSource = provisioningService.releaseTopic()
            .subscribe().atMostOnceSource();

        TestSubscriber.Probe<ProvisioningRelease> probe =
            releaseSource.runWith(TestSink.probe(server.system()), server.materializer());

        ProvisioningRelease result = probe.request(1)
            .expectNext(FiniteDuration.create(20L, TimeUnit.SECONDS));
        System.out.println(result.getStatus());

        result = probe.request(1)
            .expectNext(FiniteDuration.create(40L, TimeUnit.SECONDS));
        System.out.println(result.getStatus());

        result = probe.request(1)
            .expectNext(FiniteDuration.create(40L, TimeUnit.SECONDS));
        System.out.println(result.getStatus());
    }

    @AfterClass
    public static void tearDown() {
        if (server != null) {
            server.stop();
            server = null;
        }
    }

    private static class CoreServiceStub implements CoreService {

        @Inject
        public CoreServiceStub(ProducerStubFactory factory) {
            coreProducer = factory.producer(TOPIC_SUBSCRIPTION);
        }

        @Override
        public Topic<CoreSubscription> subscriptionTopic() {
            return coreProducer.topic();
        }
    }

    private static class RudderServiceStub implements RudderService {

        @Override
        public ServiceCall<InstallReleaseRequest, NotUsed> install() {
            return req -> CompletableFuture.completedFuture(NotUsed.getInstance());
        }

        @Override
        public ServiceCall<UpdateReleaseRequest, NotUsed> update() {
            return req -> CompletableFuture.completedFuture(NotUsed.getInstance());
        }

        @Override
        public ServiceCall<NotUsed, NotUsed> delete(String release) {
            return req -> CompletableFuture.completedFuture(NotUsed.getInstance());
        }

        @Override
        public ServiceCall<NotUsed, ReleaseStatusResponse> status(String release, String version) {
            return req -> CompletableFuture.completedFuture(new ReleaseStatusResponse("", "",
                new Info(new Status(ReleaseStatusResponse.Info.Status.DEPLOYED, ""))));
        }
    }
}
