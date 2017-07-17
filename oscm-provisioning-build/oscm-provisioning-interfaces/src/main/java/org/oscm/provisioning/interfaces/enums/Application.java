/*******************************************************************************
 *                                                                              
 *  Copyright FUJITSU LIMITED 2017                                           
 *                                                                                                                                 
 *  Creation Date: Jun 21, 2017                                                      
 *                                                                              
 *******************************************************************************/

package org.oscm.provisioning.interfaces.enums;

import org.oscm.common.interfaces.keys.ApplicationKey;

/**
 * Enum for application keys. Represents the applications / microservices that
 * this application interacts with (including itself).
 */
public enum Application implements ApplicationKey {
    PROVISIONING("provisioning", Type.INTERNAL), //
    OSCM_CORE("core", Type.INTERNAL), //
    RUDDER("rudder", Type.EXTERNAL); //

    public static final Application SELF = PROVISIONING;

    private String name;
    private Type type;

    private Application(String name, Type type) {
        this.name = name;
        this.type = type;
    }

    @Override
    public String getApplicationName() {
        return name;
    }

    @Override
    public Type getType() {
        return type;
    }

}
