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
public class Release extends Event {

    public static final String FIELD_RELEASE_REFERENCE = "release_ref";
    public static final String FIELD_STATUS = "status";
    public static final String FIELD_SERVICES = "services";

    public static final String OPTION_PENDING = "pending";
    public static final String OPTION_DEPLOYED = "deployed";
    public static final String OPTION_DELETED = "deleted";
    public static final String OPTION_FAILED = "failed";

    public enum Status {
        @SerializedName(OPTION_PENDING)
        PENDING, //

        @SerializedName(OPTION_DEPLOYED)
        DEPLOYED, //

        @SerializedName(OPTION_DELETED)
        DELETED, //

        @SerializedName(OPTION_FAILED)
        FAILED //
    }

    @SerializedName(FIELD_RELEASE_REFERENCE)
    private String releaseReference;

    @SerializedName(FIELD_STATUS)
    private Status status;

    @SerializedName(FIELD_SERVICES)
    private Map<String, String> services;

    public String getReleaseReference() {
        return releaseReference;
    }

    public void setReleaseReference(String releaseReference) {
        this.releaseReference = releaseReference;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
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
