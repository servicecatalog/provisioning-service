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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.annotation.concurrent.Immutable;
import java.util.Collections;
import java.util.Map;

/**
 * @author miethaner
 */
@Immutable
public class InstallReleaseRequest {

    public static final String FIELD_NAME = "name";
    public static final String FIELD_NAMESPACE = "namespace";
    public static final String FIELD_REPOSITORY = "repo";
    public static final String FIELD_CHART = "chart";
    public static final String FIELD_VERSION = "version";
    public static final String FIELD_VALUES = "values";

    private String name;

    private String namespace;

    private String repository;

    private String chart;

    private String version;

    private Map<String, Object> values;

    @JsonCreator
    public InstallReleaseRequest(@JsonProperty(FIELD_NAME) String name,
        @JsonProperty(FIELD_NAMESPACE) String namespace,
        @JsonProperty(FIELD_REPOSITORY) String repository,
        @JsonProperty(FIELD_CHART) String chart,
        @JsonProperty(FIELD_VERSION) String version,
        @JsonProperty(FIELD_VALUES) Map<String, Object> values) {
        this.name = name;
        this.namespace = namespace;
        this.repository = repository;
        this.chart = chart;
        this.version = version;
        this.values = values;
    }

    @JsonProperty(FIELD_NAME)
    public String getName() {
        return name;
    }

    @JsonProperty(FIELD_NAMESPACE)
    public String getNamespace() {
        return namespace;
    }

    @JsonProperty(FIELD_REPOSITORY)
    public String getRepository() {
        return repository;
    }

    @JsonProperty(FIELD_CHART)
    public String getChart() {
        return chart;
    }

    @JsonProperty(FIELD_VERSION)
    public String getVersion() {
        return version;
    }

    @JsonProperty(FIELD_VALUES)
    public Map<String, Object> getValues() {
        if (values != null) {
            return Collections.unmodifiableMap(values);
        } else {
            return Collections.emptyMap();
        }
    }
}
