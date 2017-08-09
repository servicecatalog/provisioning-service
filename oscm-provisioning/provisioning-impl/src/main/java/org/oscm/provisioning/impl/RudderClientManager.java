/*
 * ****************************************************************************
 *
 *    Copyright FUJITSU LIMITED 2017
 *
 *    Creation Date: 2017-08-04
 *
 * ****************************************************************************
 */

package org.oscm.provisioning.impl;

import akka.actor.ActorSystem;
import akka.stream.Materializer;
import com.lightbend.lagom.internal.client.CircuitBreakers;
import com.lightbend.lagom.internal.javadsl.api.broker.TopicFactoryProvider;
import com.lightbend.lagom.internal.javadsl.client.JavadslServiceClientImplementor;
import com.lightbend.lagom.internal.javadsl.client.JavadslWebSocketClient;
import com.lightbend.lagom.internal.javadsl.client.ServiceClientLoader;
import com.lightbend.lagom.javadsl.api.Descriptor;
import com.lightbend.lagom.javadsl.api.ServiceInfo;
import com.lightbend.lagom.javadsl.api.ServiceLocator;
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

@Singleton
public class RudderClientManager {

    private static class StaticServiceLocator
        extends CircuitBreakingServiceLocator {

        private final URI uri;

        StaticServiceLocator(CircuitBreakers circuitBreakers, URI uri) {
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
    private final CircuitBreakers circuitBreakers;
    private final Function<ServiceLocator, ServiceClientLoader> loaderFunction;

    private final RudderService defaultService;
    private final ConcurrentMap<URI, RudderService> services;

    @Inject
    public RudderClientManager(Environment env, WSClient wsClient,
        JavadslWebSocketClient webSocketClient, ActorSystem system,
        TopicFactoryProvider topicFactoryProvider, Materializer materializer,
        JacksonSerializerFactory serializerFactory,
        JacksonExceptionSerializer exceptionSerializer,
        CircuitBreakers circuitBreakers, ServiceLocator defaultLocator) {

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

        this.defaultService = this.loaderFunction.apply(defaultLocator)
            .loadServiceClient(RudderService.class);
    }

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
