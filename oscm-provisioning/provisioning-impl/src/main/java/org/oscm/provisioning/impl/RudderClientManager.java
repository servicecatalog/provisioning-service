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

import akka.actor.ActorSystem;
import akka.stream.Materializer;
import com.lightbend.lagom.internal.javadsl.api.broker.TopicFactoryProvider;
import com.lightbend.lagom.internal.javadsl.client.JavadslServiceClientImplementor;
import com.lightbend.lagom.internal.javadsl.client.JavadslWebSocketClient;
import com.lightbend.lagom.internal.javadsl.client.ServiceClientLoader;
import com.lightbend.lagom.javadsl.api.Descriptor;
import com.lightbend.lagom.javadsl.api.ServiceInfo;
import com.lightbend.lagom.javadsl.api.ServiceLocator;
import com.lightbend.lagom.javadsl.client.CircuitBreakersPanel;
import com.lightbend.lagom.javadsl.client.CircuitBreakingServiceLocator;
import com.lightbend.lagom.javadsl.jackson.JacksonExceptionSerializer;
import com.lightbend.lagom.javadsl.jackson.JacksonSerializerFactory;
import org.oscm.provisioning.api.ProvisioningService;
import org.oscm.rudder.api.RudderService;
import play.Environment;
import play.api.libs.ws.WSClient;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.net.URI;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

/**
 * Manager class for rudder clients.
 * <p>
 * This is necessary to get the advantages of service classes and manual service detection (which is
 * required for the rudder proxies). The manager reimplements the lagom service client, except that
 * most of the resources are already initialized and can be injected.
 */
@Singleton
public class RudderClientManager {

    /**
     * Service locator class with static URL.
     */
    private static class StaticServiceLocator
        extends CircuitBreakingServiceLocator {

        private final URI uri;

        StaticServiceLocator(CircuitBreakersPanel circuitBreakers, URI uri) {
            super(circuitBreakers);
            this.uri = uri;
        }

        @Override
        public CompletionStage<Optional<URI>> locate(String name,
            Descriptor.Call<?, ?> serviceCall) {
            return CompletableFuture.completedFuture(Optional.of(uri));
        }
    }

    private final Environment env;
    private final CircuitBreakersPanel circuitBreakers;
    private final Function<ServiceLocator, ServiceClientLoader> loaderFunction;

    private final RudderService defaultService;
    private final ConcurrentMap<URI, RudderService> services;

    @Inject
    public RudderClientManager(Environment env, WSClient wsClient,
        JavadslWebSocketClient webSocketClient, ActorSystem system,
        TopicFactoryProvider topicFactoryProvider, Materializer materializer,
        JacksonSerializerFactory serializerFactory,
        JacksonExceptionSerializer exceptionSerializer,
        CircuitBreakersPanel circuitBreakers, RudderService defaultService) {

        this.env = env;
        this.circuitBreakers = circuitBreakers;
        this.services = new ConcurrentHashMap<>();

        ServiceInfo serviceInfo = ServiceInfo
            .of(ProvisioningService.SERVICE_NAME);

        this.loaderFunction = serviceLocator -> {
            JavadslServiceClientImplementor implementor =
                new JavadslServiceClientImplementor(
                    wsClient, webSocketClient, serviceInfo, serviceLocator,
                    env.underlying(), topicFactoryProvider, system.dispatcher(),
                    materializer);
            return new ServiceClientLoader(serializerFactory,
                exceptionSerializer, env.underlying(), implementor);
        };

        this.defaultService = defaultService;
    }

    /**
     * Gets a rudder service instance for the given URI. In production, it will return a new or
     * cached instance for the URI. Otherwise, the default service is returned (with the default
     * service locator)
     *
     * @param uri
     * @return
     */
    public RudderService getServiceForURI(URI uri) {
        if (env.isProd()) {
            return services
                .computeIfAbsent(uri, fUri -> loaderFunction
                    .apply(new StaticServiceLocator(circuitBreakers, fUri))
                    .loadServiceClient(RudderService.class));
        } else {
            return defaultService;
        }
    }
}
