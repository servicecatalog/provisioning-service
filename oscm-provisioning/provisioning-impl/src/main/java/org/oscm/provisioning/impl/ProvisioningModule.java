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

import com.google.inject.AbstractModule;
import com.lightbend.lagom.javadsl.server.ServiceGuiceSupport;
import org.oscm.core.api.CoreService;
import org.oscm.provisioning.api.ProvisioningService;
import org.oscm.rudder.api.RudderService;

/**
 * Guice module for binding classes and resources for the provisioning service.
 */
public class ProvisioningModule extends AbstractModule implements
    ServiceGuiceSupport {

    @Override
    protected void configure() {
        bindService(ProvisioningService.class, ProvisioningServiceImpl.class);
        bindClient(CoreService.class);
        bindClient(RudderService.class);
        bind(ReleaseScheduler.class).asEagerSingleton();
    }
}
