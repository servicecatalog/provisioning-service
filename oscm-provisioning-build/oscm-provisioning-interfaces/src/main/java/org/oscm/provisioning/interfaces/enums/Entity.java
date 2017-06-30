/*******************************************************************************
 *                                                                              
 *  Copyright FUJITSU LIMITED 2017                                           
 *                                                                                                                                 
 *  Creation Date: Jun 28, 2017                                                      
 *                                                                              
 *******************************************************************************/

package org.oscm.provisioning.interfaces.enums;

import org.oscm.common.interfaces.data.Event;
import org.oscm.common.interfaces.keys.ApplicationKey;
import org.oscm.common.interfaces.keys.EntityKey;
import org.oscm.provisioning.interfaces.data.Release;
import org.oscm.provisioning.interfaces.data.Subscription;

/**
 * Enum for entity keys. Represents entities that this application interacts
 * with.
 */
public enum Entity implements EntityKey {
    SUBSCRIPTION("subscription", Subscription.class, Application.OSCM_CORE), //
    RELEASE("release", Release.class, Application.SELF); //

    private String name;
    private Class<? extends Event> clazz;
    private ApplicationKey application;

    private Entity(String name, Class<? extends Event> clazz,
            ApplicationKey application) {
        this.name = name;
        this.clazz = clazz;
        this.application = application;
    }

    @Override
    public String getEntityName() {
        return name;
    }

    @Override
    public Class<? extends Event> getEntityClass() {
        return clazz;
    }

    @Override
    public ApplicationKey getApplication() {
        return application;
    }

}
