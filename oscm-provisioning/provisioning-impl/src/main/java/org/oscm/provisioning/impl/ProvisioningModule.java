/*
 * ****************************************************************************
 *
 *    Copyright FUJITSU LIMITED 2017
 *
 *    Creation Date: 2017-08-02
 *
 * ****************************************************************************
 */

package org.oscm.provisioning.impl;

import com.google.inject.AbstractModule;
import com.lightbend.lagom.javadsl.server.ServiceGuiceSupport;
import org.oscm.core.api.CoreService;
import org.oscm.provisioning.api.ProvisioningService;

public class ProvisioningModule extends AbstractModule implements
    ServiceGuiceSupport {

    @Override
    protected void configure() {
        bindService(ProvisioningService.class, ProvisioningServiceImpl.class);
        bindClient(CoreService.class);
        bind(ReleaseScheduler.class).asEagerSingleton();
    }
}
