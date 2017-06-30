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
    PROVISIONING("provisioning"), //
    OSCM_CORE("core"); //

    public static final Application SELF = PROVISIONING;

    private String name;

    private Application(String name) {
        this.name = name;
    }

    @Override
    public String getApplicationName() {
        return name;
    }

}
