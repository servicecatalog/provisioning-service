/*******************************************************************************
 *                                                                              
 *  Copyright FUJITSU LIMITED 2017                                           
 *                                                                                                                                 
 *  Creation Date: Jul 18, 2017                                                      
 *                                                                              
 *******************************************************************************/

package org.oscm.provisioning.interfaces.data;

import com.google.gson.annotations.SerializedName;

/**
 * @author miethaner
 *
 */
public class Template {

    public static final String FIELD_REPOSITORY = "repository";
    public static final String FIELD_NAME = "name";
    public static final String FIELD_TEMPLATE_VERSION = "version";

    @SerializedName(FIELD_REPOSITORY)
    private String repository;

    @SerializedName(FIELD_NAME)
    private String name;

    @SerializedName(FIELD_TEMPLATE_VERSION)
    private String version;

    public String getRepository() {
        return repository;
    }

    public void setRepository(String repository) {
        this.repository = repository;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
