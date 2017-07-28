/*******************************************************************************
 *                                                                              
 *  Copyright FUJITSU LIMITED 2017                                           
 *                                                                                                                                 
 *  Creation Date: Jun 30, 2017                                                      
 *                                                                              
 *******************************************************************************/

package org.oscm.provisioning.interfaces.data;

import java.util.Map;

import org.oscm.common.interfaces.data.Event;
import org.oscm.common.interfaces.data.Failure;
import org.oscm.common.interfaces.exceptions.ServiceException;
import org.oscm.common.interfaces.keys.ActivityKey;

import com.google.gson.annotations.SerializedName;

/**
 * @author miethaner
 *
 */
public class Release extends Event {

    public static final String FIELD_STATUS = "status";
    public static final String FIELD_FAILURE = "failure";
    public static final String FIELD_INSTANCE = "instance";
    public static final String FIELD_SERVICES = "services";
    public static final String FIELD_TARGET = "target";
    public static final String FIELD_NAMESPACE = "namespace";
    public static final String FIELD_TEMPLATE = "template";
    public static final String FIELD_PARAMETERS = "parameters";

    public static final String OPTION_CREATNG = "creating";
    public static final String OPTION_UPDATING = "updating";
    public static final String OPTION_DELETING = "deleting";
    public static final String OPTION_PENDING = "pending";
    public static final String OPTION_DEPLOYED = "deployed";
    public static final String OPTION_DELETED = "deleted";
    public static final String OPTION_FAILED = "failed";

    public enum Status {
        @SerializedName(OPTION_CREATNG)
        CREATING, //

        @SerializedName(OPTION_UPDATING)
        UPDATING, //

        @SerializedName(OPTION_DELETING)
        DELETING, //

        @SerializedName(OPTION_PENDING)
        PENDING, //

        @SerializedName(OPTION_DEPLOYED)
        DEPLOYED, //

        @SerializedName(OPTION_DELETED)
        DELETED, //

        @SerializedName(OPTION_FAILED)
        FAILED //
    }

    @SerializedName(FIELD_STATUS)
    private Status status;

    @SerializedName(FIELD_FAILURE)
    private Failure failure;

    @SerializedName(FIELD_TARGET)
    private String target;

    @SerializedName(FIELD_TEMPLATE)
    private Template template;

    @SerializedName(FIELD_NAMESPACE)
    private String namespace;

    @SerializedName(FIELD_INSTANCE)
    private String instance;

    @SerializedName(FIELD_PARAMETERS)
    private Map<String, Object> parameters;

    @SerializedName(FIELD_SERVICES)
    private Map<String, String> services;

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Failure getFailure() {
        return failure;
    }

    public void setFailure(Failure failure) {
        this.failure = failure;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public Template getTemplate() {
        return template;
    }

    public void setTemplate(Template template) {
        this.template = template;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getInstance() {
        return instance;
    }

    public void setInstance(String instance) {
        this.instance = instance;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }

    public Map<String, String> getServices() {
        return services;
    }

    public void setServices(Map<String, String> services) {
        this.services = services;
    }

    @Override
    public void validateFor(ActivityKey activity) throws ServiceException {
        // nothing to validate
    }
}
