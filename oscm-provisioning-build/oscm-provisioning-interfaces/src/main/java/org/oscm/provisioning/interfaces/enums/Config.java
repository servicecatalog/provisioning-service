/*******************************************************************************
 *                                                                              
 *  Copyright FUJITSU LIMITED 2017                                           
 *                                                                                                                                 
 *  Creation Date: Jun 29, 2017                                                      
 *                                                                              
 *******************************************************************************/

package org.oscm.provisioning.interfaces.enums;

import org.oscm.common.interfaces.keys.ConfigurationKey;

/**
 * Enum for configuration keys. Represents application specific configuration settings.
 */
public enum Config implements ConfigurationKey {
    CONFIG("config", "con.fig", false, "42");

    private String name;
    private String proprietary;
    private boolean mandatory;
    private String defaultValue;

    private Config(String name, String proprietary, boolean mandatory,
            String defaultValue) {
        this.name = name;
        this.proprietary = proprietary;
        this.mandatory = mandatory;
        this.defaultValue = defaultValue;
    }

    @Override
    public String getConfigurationName() {
        return name;
    }

    @Override
    public String getProprietaryName() {
        return proprietary;
    }

    @Override
    public boolean isMandatory() {
        return mandatory;
    }

    @Override
    public String getDefaultValue() {
        return defaultValue;
    }
}
