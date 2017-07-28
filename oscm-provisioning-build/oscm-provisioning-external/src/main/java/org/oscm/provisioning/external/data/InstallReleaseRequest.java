/*******************************************************************************
 *                                                                              
 *  Copyright FUJITSU LIMITED 2017                                           
 *                                                                                                                                 
 *  Creation Date: Jul 7, 2017                                                      
 *                                                                              
 *******************************************************************************/

package org.oscm.provisioning.external.data;

import java.util.Map;

import com.google.gson.annotations.SerializedName;

/**
 * @author miethaner
 *
 */
public class InstallReleaseRequest {

    public static final String FIELD_NAME = "name";
    public static final String FIELD_NAMESPACE = "namespace";
    public static final String FIELD_REPOSITORY = "repo";
    public static final String FIELD_CHART = "chart";
    public static final String FIELD_VERSION = "version";
    public static final String FIELD_VALUES = "values";

    @SerializedName(FIELD_NAME)
    private String name;

    @SerializedName(FIELD_NAMESPACE)
    private String namespace;

    @SerializedName(FIELD_REPOSITORY)
    private String repository;

    @SerializedName(FIELD_CHART)
    private String chart;

    @SerializedName(FIELD_VERSION)
    private String version;

    @SerializedName(FIELD_VALUES)
    private Map<String, Object> values;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getRepository() {
        return repository;
    }

    public void setRepository(String repository) {
        this.repository = repository;
    }

    public String getChart() {
        return chart;
    }

    public void setChart(String chart) {
        this.chart = chart;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Map<String, Object> getValues() {
        return values;
    }

    public void setValues(Map<String, Object> values) {
        this.values = values;
    }
}
