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
import org.oscm.lagom.data.Failure;
import org.oscm.lagom.data.Identity;

import javax.annotation.concurrent.Immutable;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

/**
 * API release entity class exposed by the provisioning.
 *
 * @author miethaner
 */

@Immutable
public class ProvisioningRelease extends Identity {

    public static final String FIELD_TARGET = "target";
    public static final String FIELD_NAMESPACE = "namespace";
    public static final String FIELD_TEMPLATE = "template";
    public static final String FIELD_LABELS = "labels";
    public static final String FIELD_PARAMETERS = "parameters";
    public static final String FIELD_STATUS = "status";
    public static final String FIELD_FAILURE = "failure";
    public static final String FIELD_INSTANCE = "instance";
    public static final String FIELD_ENDPOINTS = "endpoints";

    public static final String OPTION_PENDING = "pending";
    public static final String OPTION_DEPLOYED = "deployed";
    public static final String OPTION_DELETED = "deleted";
    public static final String OPTION_FAILED = "failed";

    public static class Template {

        public static final String FIELD_REPOSITORY = "repository";
        public static final String FIELD_NAME = "name";
        public static final String FIELD_TEMPLATE_VERSION = "version";

        private String repository;
        private String name;
        private String version;

        @JsonCreator
        public Template(@JsonProperty(FIELD_REPOSITORY) String repository,
            @JsonProperty(FIELD_NAME) String name,
            @JsonProperty(FIELD_TEMPLATE_VERSION) String version) {
            this.repository = repository;
            this.name = name;
            this.version = version;
        }

        public String getRepository() {
            return repository;
        }

        public String getName() {
            return name;
        }

        public String getVersion() {
            return version;
        }
    }

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

    @JsonCreator
    public ProvisioningRelease(@JsonProperty(FIELD_ID) UUID id,
        @JsonProperty(FIELD_TIMESTAMP) Long timestamp,
        @JsonProperty(FIELD_TARGET) String target,
        @JsonProperty(FIELD_NAMESPACE) String namespace,
        @JsonProperty(FIELD_TEMPLATE) Template template,
        @JsonProperty(FIELD_LABELS) Map<String, String> labels,
        @JsonProperty(FIELD_PARAMETERS) Map<String, Object> parameters,
        @JsonProperty(FIELD_STATUS) Status status,
        @JsonProperty(FIELD_FAILURE) Failure failure,
        @JsonProperty(FIELD_INSTANCE) String instance,
        @JsonProperty(FIELD_ENDPOINTS) Map<String, String> endpoints) {
        super(id, timestamp);
        this.target = target;
        this.namespace = namespace;
        this.template = template;
        this.labels = labels;
        this.parameters = parameters;
        this.status = status;
        this.failure = failure;
        this.instance = instance;
        this.endpoints = endpoints;
    }

    private String target;

    private String namespace;

    private Template template;

    private Map<String, String> labels;

    private Map<String, Object> parameters;

    private Status status;

    private Failure failure;

    private String instance;

    private Map<String, String> endpoints;

    @JsonProperty(FIELD_TARGET)
    public String getTarget() {
        return target;
    }

    @JsonProperty(FIELD_NAMESPACE)
    public String getNamespace() {
        return namespace;
    }

    @JsonProperty(FIELD_TEMPLATE)
    public Template getTemplate() {
        return template;
    }

    @JsonProperty(FIELD_LABELS)
    public Map<String, String> getLabels() {
        if (labels != null) {
            return Collections.unmodifiableMap(labels);
        } else {
            return Collections.emptyMap();
        }
    }

    @JsonProperty(FIELD_PARAMETERS)
    public Map<String, Object> getParameters() {
        if (parameters != null) {
            return Collections.unmodifiableMap(parameters);
        } else {
            return Collections.emptyMap();
        }
    }

    @JsonProperty(FIELD_STATUS)
    public Status getStatus() {
        return status;
    }

    @JsonProperty(FIELD_FAILURE)
    public Failure getFailure() {
        return failure;
    }

    @JsonProperty(FIELD_INSTANCE)
    public String getInstance() {
        return instance;
    }

    @JsonProperty(FIELD_ENDPOINTS)
    public Map<String, String> getEndpoints() {
        if (endpoints != null) {
            return Collections.unmodifiableMap(endpoints);
        } else {
            return Collections.emptyMap();
        }
    }
}
