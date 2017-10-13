/*
 * ****************************************************************************
 *
 *    Copyright FUJITSU LIMITED 2017
 *
 *    Creation Date: 2017-08-02
 *
 * ****************************************************************************
 */

package org.oscm.core.api.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.oscm.lagom.data.Identity;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

/**
 * <b>Public</b> data class for the subscription topic of the core service.
 */
public class CoreSubscription extends Identity {

    public static final String FIELD_OPERATION = "operation";
    public static final String FIELD_TARGET = "target";
    public static final String FIELD_NAMESPACE = "namespace";
    public static final String FIELD_TEMPLATE = "template";
    public static final String FIELD_LABELS = "labels";
    public static final String FIELD_PARAMETERS = "parameters";
    public static final String FIELD_ENDPOINTS = "endpoints";

    public static final String OPTION_UPDATE = "upd";
    public static final String OPTION_DELETE = "del";

    /**
     * Basic commands for subscriptions
     */
    public enum Operation {
        @JsonProperty(OPTION_UPDATE)
        UPDATE,

        @JsonProperty(OPTION_DELETE)
        DELETE
    }

    /**
     * Helm chart information
     */
    public static class Template {

        public static final String FIELD_REPOSITORY = "repository";
        public static final String FIELD_NAME = "name";
        public static final String FIELD_TEMPLATE_VERSION = "version";

        private String repository;
        private String name;
        private String version;

        /**
         * Creates a new template.
         *
         * @param repository the chart repository, null returns null
         * @param name       the chart name, null returns null
         * @param version    the chart version, null returns null
         */
        @JsonCreator
        public Template(@JsonProperty(FIELD_REPOSITORY) String repository,
            @JsonProperty(FIELD_NAME) String name,
            @JsonProperty(FIELD_TEMPLATE_VERSION) String version) {
            this.repository = repository;
            this.name = name;
            this.version = version;
        }

        /**
         * Gets the charts repository.
         *
         * @return the repository, null if not set
         */
        public String getRepository() {
            return repository;
        }

        /**
         * Gets the charts name.
         *
         * @return the name, null if not set
         */
        public String getName() {
            return name;
        }

        /**
         * Gets the charts version.
         *
         * @return the version, null if not set
         */
        public String getVersion() {
            return version;
        }
    }

    private Operation operation;
    private String target;
    private String namespace;
    private Template template;
    private Map<String, String> labels;
    private Map<String, Object> parameters;
    private Map<String, String> endpoints;

    /**
     * Creates a new subscription.
     *
     * @param id         the entities id, null returns null
     * @param timestamp  the entities creation timestamp, null returns null
     * @param operation  the operation for the subscription, null returns null
     * @param target     the URL of the target rudder proxy, null returns null
     * @param namespace  the name space to deploy to, null returns null
     * @param template   the helm chart to use, null returns null
     * @param labels     the labels to be attached to the deployment, null returns empty map
     * @param parameters the parameters to be used for the deployment, null returns empty map
     * @param endpoints  the endpoint templates for the deployment, null returns empty map
     */
    @JsonCreator
    public CoreSubscription(@JsonProperty(FIELD_ID) UUID id,
        @JsonProperty(FIELD_TIMESTAMP) Long timestamp,
        @JsonProperty(FIELD_OPERATION) Operation operation,
        @JsonProperty(FIELD_TARGET) String target,
        @JsonProperty(FIELD_NAMESPACE) String namespace,
        @JsonProperty(FIELD_TEMPLATE) Template template,
        @JsonProperty(FIELD_LABELS) Map<String, String> labels,
        @JsonProperty(FIELD_PARAMETERS) Map<String, Object> parameters,
        @JsonProperty(FIELD_ENDPOINTS) Map<String, String> endpoints) {
        super(id, timestamp);
        this.operation = operation;
        this.target = target;
        this.namespace = namespace;
        this.template = template;
        this.labels = labels;
        this.parameters = parameters;
        this.endpoints = endpoints;
    }

    /**
     * Gets the basic operation for this event.
     *
     * @return the operation, null if not set
     */
    public Operation getOperation() {
        return operation;
    }

    /**
     * Gets the URL of the target Rudder proxy.
     *
     * @return the target URL, null if not set
     */
    public String getTarget() {
        return target;
    }

    /**
     * Gets the namespace to deploy to.
     *
     * @return the namespace, null if not set
     */
    public String getNamespace() {
        return namespace;
    }

    /**
     * Gets the location, name and version of the helm chart.
     *
     * @return the helm chart template, null if not set
     */
    public Template getTemplate() {
        return template;
    }

    /**
     * Gets the labels that should be attached to the deployment.
     *
     * @return the labels
     */
    public Map<String, String> getLabels() {
        if (labels != null) {
            return Collections.unmodifiableMap(labels);
        } else {
            return Collections.emptyMap();
        }
    }

    /**
     * Gets the parameters (values) for the deployment.
     *
     * @return the parameters
     */
    public Map<String, Object> getParameters() {
        if (parameters != null) {
            return Collections.unmodifiableMap(parameters);
        } else {
            return Collections.emptyMap();
        }
    }

    /**
     * Gets the endpoint templates for the deployment.
     *
     * @return the endpoints
     */
    public Map<String, String> getEndpoints() {
        if (endpoints != null) {
            return Collections.unmodifiableMap(endpoints);
        } else {
            return Collections.emptyMap();
        }
    }
}
