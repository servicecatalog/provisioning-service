/*******************************************************************************
 *                                                                              
 *  Copyright FUJITSU LIMITED 2017                                           
 *                                                                                                                                 
 *  Creation Date: Jul 6, 2017                                                      
 *                                                                              
 *******************************************************************************/

package org.oscm.provisioning.interfaces.enums;

import org.oscm.common.interfaces.data.Version;
import org.oscm.common.interfaces.keys.ConfigurationKey;

/**
 * @author miethaner
 *
 */
public enum Config implements ConfigurationKey {
    ;

    public static final Version LATEST_VERSION = new Version(1, 0, 0);

    private String name;
    private boolean mandatory;
    private String defaultValue;

    private Config(String name, boolean mandatory, String defaultValue) {
        this.name = name;
        this.mandatory = mandatory;
        this.defaultValue = defaultValue;
    }

    @Override
    public String getConfigurationName() {
        return name;
    }

    @Override
    public String getProprietaryName() {
        return "";
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
