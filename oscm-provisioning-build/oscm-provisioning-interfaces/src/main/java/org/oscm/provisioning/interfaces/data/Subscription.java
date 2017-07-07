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
import org.oscm.common.interfaces.exceptions.ServiceException;
import org.oscm.common.interfaces.keys.ActivityKey;

import com.google.gson.annotations.SerializedName;

/**
 * @author miethaner
 *
 */
public class Subscription extends Event {

    public static final String FIELD_TARGET = "target";
    public static final String FIELD_TEMPLATE = "template";
    public static final String FIELD_LABELS = "labels";
    public static final String FIELD_PARAMETERS = "parameters";

    @SerializedName(FIELD_TARGET)
    private String target;

    @SerializedName(FIELD_TEMPLATE)
    private String template;

    @SerializedName(FIELD_LABELS)
    private Map<String, String> labels;

    @SerializedName(FIELD_PARAMETERS)
    private Map<String, String> parameters;

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public Map<String, String> getLabels() {
        return labels;
    }

    public void setLabels(Map<String, String> labels) {
        this.labels = labels;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    @Override
    public void validateFor(ActivityKey activity) throws ServiceException {
        // nothing to validate
    }
}
