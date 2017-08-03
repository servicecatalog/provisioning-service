/*
 * ****************************************************************************
 *
 *    Copyright FUJITSU LIMITED 2017
 *
 *    Creation Date: 2017-08-03
 *
 * ****************************************************************************
 */

package org.oscm.provisioning.impl.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.oscm.core.api.data.CoreSubscription;

import javax.annotation.concurrent.Immutable;
import java.util.Collections;
import java.util.Map;

@Immutable
public class Release {

    public static final String FIELD_TARGET = "target";
    public static final String FIELD_NAMESPACE = "namespace";
    public static final String FIELD_REPOSITORY = "repository";
    public static final String FIELD_TEMPLATE = "template";
    public static final String FIELD_VERSION = "version";
    public static final String FIELD_LABELS = "labels";
    public static final String FIELD_PARAMETERS = "parameters";

    @JsonCreator
    public Release(@JsonProperty(FIELD_TARGET) String target,
        @JsonProperty(FIELD_NAMESPACE) String namespace,
        @JsonProperty(FIELD_REPOSITORY) String repository,
        @JsonProperty(FIELD_TEMPLATE) String template,
        @JsonProperty(FIELD_VERSION) String version,
        @JsonProperty(FIELD_LABELS) Map<String, String> labels,
        @JsonProperty(FIELD_PARAMETERS) Map<String, Object> parameters) {
        this.target = target;
        this.namespace = namespace;
        this.repository = repository;
        this.template = template;
        this.version = version;
        this.labels = labels;
        this.parameters = parameters;
    }

    public Release(CoreSubscription subscription) {
        if (subscription == null) {
            return;
        }

        this.target = subscription.getTarget();
        this.namespace = subscription.getNamespace();
        this.labels = subscription.getLabels();
        this.parameters = subscription.getParameters();

        if (subscription.getTemplate() != null) {
            this.repository = subscription.getTemplate().getRepository();
            this.template = subscription.getTemplate().getName();
            this.version = subscription.getTemplate().getVersion();
        }
    }

    private String target;

    private String namespace;

    private String repository;

    private String template;

    private String version;

    private Map<String, String> labels;

    private Map<String, Object> parameters;

    @JsonProperty(FIELD_TARGET)
    public String getTarget() {
        return target;
    }

    @JsonProperty(FIELD_NAMESPACE)
    public String getNamespace() {
        return namespace;
    }

    @JsonProperty(FIELD_REPOSITORY)
    public String getRepository() {
        return repository;
    }

    @JsonProperty(FIELD_TEMPLATE)
    public String getTemplate() {
        return template;
    }

    @JsonProperty(FIELD_VERSION)
    public String getVersion() {
        return version;
    }

    @JsonProperty(FIELD_LABELS)
    public Map<String, String> getLabels() {
        return Collections.unmodifiableMap(labels);
    }

    @JsonProperty(FIELD_PARAMETERS)
    public Map<String, Object> getParameters() {
        return Collections.unmodifiableMap(parameters);
    }
}
