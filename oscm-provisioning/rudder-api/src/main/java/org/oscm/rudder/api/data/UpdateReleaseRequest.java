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

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.Map;

/**
 * <b>Public</b> data class for the update endpoint of the rudder service.
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

    /**
     * Creates a new immutable update request for a helm release.
     *
     * @param name       the name of the release
     * @param repository the repository of the helm chart
     * @param chart      the name of the helm chart
     * @param version    the version of the helm chart
     * @param values     the parameters of the helm chart (must correspond to the structure in the values.yaml/.json
     */
    public UpdateReleaseRequest(String name, String repository, String chart, String version,
        Map<String, Object> values) {
        this.name = name;
        this.repository = repository;
        this.chart = chart;
        this.version = version;
        this.values = values;
    }

    /**
     * Gets the name of the release (instance id).
     *
     * @return the name, null if not set
     */
    @JsonProperty(FIELD_NAME)
    public String getName() {
        return name;
    }

    /**
     * Gets the helm charts repository.
     *
     * @return the repository, null if not set
     */
    @JsonProperty(FIELD_REPOSITORY)
    public String getRepository() {
        return repository;
    }

    /**
     * Gets the helm charts name.
     *
     * @return the chart, null if not set
     */
    @JsonProperty(FIELD_CHART)
    public String getChart() {
        return chart;
    }

    /**
     * Gets the helm charts version.
     *
     * @return the version, null if not set
     */
    @JsonProperty(FIELD_VERSION)
    public String getVersion() {
        return version;
    }

    /**
     * Gets the values for the helm chart.
     *
     * @return the values
     */
    @JsonProperty(FIELD_VALUES)
    public Map<String, Object> getValues() {
        if (values != null) {
            return Collections.unmodifiableMap(values);
        } else {
            return Collections.emptyMap();
        }
    }
}
