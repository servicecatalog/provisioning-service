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

import com.lightbend.lagom.javadsl.client.integration.LagomClientFactory;
import org.oscm.provisioning.api.ProvisioningService;
import org.oscm.rudder.api.RudderService;
import play.Environment;
import play.inject.ApplicationLifecycle;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.net.URI;
import java.util.concurrent.CompletableFuture;

@Singleton
public class RudderClientManager {

    private final LagomClientFactory factory;
    private final Environment env;

    @Inject
    public RudderClientManager(ApplicationLifecycle lifecycle,
        Environment env) {
        this.factory = LagomClientFactory
            .create(ProvisioningService.SERVICE_NAME,
                LagomClientFactory.class.getClassLoader());
        this.env = env;

        lifecycle.addStopHook(() -> {
            factory.close();
            return CompletableFuture.completedFuture(null);
        });
    }

    public RudderService getServiceForURI(URI uri) {
        if (env.isProd()) {
            return factory.createClient(RudderService.class, uri);
        } else {
            return factory.createDevClient(RudderService.class);
        }
    }
}
