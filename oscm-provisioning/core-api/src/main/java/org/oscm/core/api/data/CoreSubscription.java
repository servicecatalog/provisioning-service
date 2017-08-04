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

import javax.annotation.concurrent.Immutable;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

@Immutable
public class CoreSubscription extends Identity {

    public static final String FIELD_OPERATION = "operation";
    public static final String FIELD_TARGET = "target";
    public static final String FIELD_NAMESPACE = "namespace";
    public static final String FIELD_TEMPLATE = "template";
    public static final String FIELD_LABELS = "labels";
    public static final String FIELD_PARAMETERS = "parameters";

    public static final String OPTION_UPDATE = "upd";
    public static final String OPTION_DELETE = "del";

    public enum Operation {
        @JsonProperty(OPTION_UPDATE)
        UPDATE,

        @JsonProperty(OPTION_DELETE)
        DELETE
    }

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

    private Operation operation;
    private String target;
    private String namespace;
    private Template template;
    private Map<String, String> labels;
    private Map<String, Object> parameters;

    @JsonCreator
    public CoreSubscription(@JsonProperty(FIELD_ID) UUID id,
        @JsonProperty(FIELD_TIMESTAMP) Long timestamp,
        @JsonProperty(FIELD_OPERATION) Operation operation,
        @JsonProperty(FIELD_TARGET) String target,
        @JsonProperty(FIELD_NAMESPACE) String namespace,
        @JsonProperty(FIELD_TEMPLATE) Template template,
        @JsonProperty(FIELD_LABELS) Map<String, String> labels,
        @JsonProperty(FIELD_PARAMETERS) Map<String, Object> parameters) {
        super(id, timestamp);
        this.operation = operation;
        this.target = target;
        this.namespace = namespace;
        this.template = template;
        this.labels = labels;
        this.parameters = parameters;
    }

    public Operation getOperation() {
        return operation;
    }

    public String getTarget() {
        return target;
    }

    public String getNamespace() {
        return namespace;
    }

    public Template getTemplate() {
        return template;
    }

    public Map<String, String> getLabels() {
        return Collections.unmodifiableMap(labels);
    }

    public Map<String, Object> getParameters() {
        return Collections.unmodifiableMap(parameters);
    }
}
