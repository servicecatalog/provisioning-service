/*
 * ****************************************************************************
 *
 *    Copyright FUJITSU LIMITED 2017
 *
 *    Creation Date: 2017-09-29
 *
 * ****************************************************************************
 */

package org.oscm.provisioning.api.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.oscm.lagom.data.Identity;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

/**
 * <b>Public</b> data class for the release topic of the provisioning service.
 */
public class ProvisioningRelease extends Identity {

    public static final String FIELD_TARGET = "target";
    public static final String FIELD_NAMESPACE = "namespace";
    public static final String FIELD_TEMPLATE = "template";
    public static final String FIELD_LABELS = "labels";
    public static final String FIELD_PARAMETERS = "parameters";
    public static final String FIELD_STATUS = "status";
    public static final String FIELD_REASON = "reason";
    public static final String FIELD_INSTANCE_ID = "instance_id";
    public static final String FIELD_ENDPOINTS = "endpoints";

    public static final String OPTION_PENDING = "pending";
    public static final String OPTION_DEPLOYED = "deployed";
    public static final String OPTION_DELETED = "deleted";
    public static final String OPTION_FAILED = "failed";

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

    /**
     * The statuses for releases
     */
    public enum Status {
        @JsonProperty(OPTION_PENDING)
        PENDING, //

        @JsonProperty(OPTION_DEPLOYED)
        DEPLOYED, //

        @JsonProperty(OPTION_DELETED)
        DELETED, //

        @JsonProperty(OPTION_FAILED)
        FAILED //
    }

    private String target;

    private String namespace;

    private Template template;

    private Map<String, String> labels;

    private Map<String, Object> parameters;

    private Status status;

    private String reason;

    private String instanceId;

    private Map<String, String> endpoints;

    /**
     * Creates a new release.
     *
     * @param id         the entities id, null returns null
     * @param timestamp  the entities creation timestamp, null returns null
     * @param target     the target URL of the rudder proxy, null returns null
     * @param namespace  the namespace for the release, null returns null
     * @param template   the helm chart information, null returns null
     * @param labels     the labels for the release, null returns empty map
     * @param parameters the parameters for the release, null returns empty map
     * @param status     the status of the release, null returns null
     * @param reason     the reason of any existing failure, null returns null
     * @param instanceId the instance id of the release, null returns null
     * @param endpoints  the endpoints of the release, null returns empty map
     */
    @JsonCreator
    public ProvisioningRelease(@JsonProperty(FIELD_ID) UUID id,
        @JsonProperty(FIELD_TIMESTAMP) Long timestamp,
        @JsonProperty(FIELD_TARGET) String target,
        @JsonProperty(FIELD_NAMESPACE) String namespace,
        @JsonProperty(FIELD_TEMPLATE) Template template,
        @JsonProperty(FIELD_LABELS) Map<String, String> labels,
        @JsonProperty(FIELD_PARAMETERS) Map<String, Object> parameters,
        @JsonProperty(FIELD_STATUS) Status status,
        @JsonProperty(FIELD_REASON) String reason,
        @JsonProperty(FIELD_INSTANCE_ID) String instanceId,
        @JsonProperty(FIELD_ENDPOINTS) Map<String, String> endpoints) {
        super(id, timestamp);
        this.target = target;
        this.namespace = namespace;
        this.template = template;
        this.labels = labels;
        this.parameters = parameters;
        this.status = status;
        this.reason = reason;
        this.instanceId = instanceId;
        this.endpoints = endpoints;
    }

    /**
     * Gets the URL of the target rudder proxy.
     *
     * @return the target, null if not set
     */
    @JsonProperty(FIELD_TARGET)
    public String getTarget() {
        return target;
    }

    /**
     * Gets the namespace to be deployed to.
     *
     * @return the namespace, null if not set
     */
    @JsonProperty(FIELD_NAMESPACE)
    public String getNamespace() {
        return namespace;
    }

    /**
     * Gets the helm chart information.
     *
     * @return the template, null if not set
     */
    @JsonProperty(FIELD_TEMPLATE)
    public Template getTemplate() {
        return template;
    }

    /**
     * Gets the labels for the release.
     *
     * @return the labels
     */
    @JsonProperty(FIELD_LABELS)
    public Map<String, String> getLabels() {
        if (labels != null) {
            return Collections.unmodifiableMap(labels);
        } else {
            return Collections.emptyMap();
        }
    }

    /**
     * Gets the parameters for the release.
     *
     * @return the parameters
     */
    @JsonProperty(FIELD_PARAMETERS)
    public Map<String, Object> getParameters() {
        if (parameters != null) {
            return Collections.unmodifiableMap(parameters);
        } else {
            return Collections.emptyMap();
        }
    }

    /**
     * Gets the status of the release.
     *
     * @return the status, null if not set
     */
    @JsonProperty(FIELD_STATUS)
    public Status getStatus() {
        return status;
    }

    /**
     * Gets the reason for any failure.
     *
     * @return the reason, null if not set
     */
    @JsonProperty(FIELD_REASON)
    public String getReason() {
        return reason;
    }

    /**
     * Gets the instance id for the release if existing.
     *
     * @return the instance id, null if not set
     */
    @JsonProperty(FIELD_INSTANCE_ID)
    public String getInstanceId() {
        return instanceId;
    }

    /**
     * Gets the available endpoints for the release
     *
     * @return the endpoints
     */
    @JsonProperty(FIELD_ENDPOINTS)
    public Map<String, String> getEndpoints() {
        if (endpoints != null) {
            return Collections.unmodifiableMap(endpoints);
        } else {
            return Collections.emptyMap();
        }
    }
}
