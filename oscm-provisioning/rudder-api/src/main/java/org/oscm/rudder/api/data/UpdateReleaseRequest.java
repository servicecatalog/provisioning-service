/*
 * ****************************************************************************
 *
 *    Copyright FUJITSU LIMITED 2017
 *
 *    Creation Date: 2017-08-02
 *
 * ****************************************************************************
 */

package org.oscm.rudder.api.data;

import java.util.Map;

/**
 * @author miethaner
 */
public class UpdateReleaseRequest {

    public static final String FIELD_NAME = "name";
    public static final String FIELD_REPOSITORY = "repo";
    public static final String FIELD_CHART = "chart";
    public static final String FIELD_VERSION = "version";
    public static final String FIELD_VALUES = "values";

    private String name;

    private String repository;

    private String chart;

    private String version;

    private Map<String, Object> values;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
