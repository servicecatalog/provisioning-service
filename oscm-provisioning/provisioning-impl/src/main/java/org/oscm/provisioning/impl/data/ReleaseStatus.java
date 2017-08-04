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

import com.fasterxml.jackson.annotation.JsonProperty;

public enum ReleaseStatus {

    @JsonProperty(Constants.OPTION_NONE)
    NONE,

    @JsonProperty(Constants.OPTION_INSTALLING)
    INSTALLING, //

    @JsonProperty(Constants.OPTION_UPDATING)
    UPDATING, //

    @JsonProperty(Constants.OPTION_DELETING)
    DELETING, //

    @JsonProperty(Constants.OPTION_DEPLOYED)
    DEPLOYED, //

    @JsonProperty(Constants.OPTION_DELETED)
    DELETED, //

    @JsonProperty(Constants.OPTION_FAILED)
    FAILED, //

    @JsonProperty(Constants.OPTION_ERROR)
    ERROR; //

    public static class Constants {
        public static final String OPTION_NONE = "none";
        public static final String OPTION_INSTALLING = "installing";
        public static final String OPTION_UPDATING = "updating";
        public static final String OPTION_DELETING = "deleting";
        public static final String OPTION_DEPLOYED = "deployed";
        public static final String OPTION_DELETED = "deleted";
        public static final String OPTION_FAILED = "failed";
        public static final String OPTION_ERROR = "error";
    }
}
