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

/**
 * <b>Public</b> data class of the status endpoint of the rudder service.
 */
public class ReleaseStatusResponse {

    public static class Info {

        public static class Status {

            //Status codes for the helm deployments
            public static final int UNKNOWN = 0;
            public static final int DEPLOYED = 1;
            public static final int DELETED = 2;
            public static final int SUPERSEDED = 3;
            public static final int FAILED = 4;

            public static final String FIELD_CODE = "code";
            public static final String FIELD_RESOURCES = "resource";

            private Integer code;

            private String resources;

            /**
             * Creates a new status info.
             *
             * @param code      the status code, null returns null
             * @param resources the kubectl resource output, null returns null
             */
            @JsonCreator
            public Status(@JsonProperty(FIELD_CODE) Integer code,
                @JsonProperty(FIELD_RESOURCES) String resources) {
                this.code = code;
                this.resources = resources;
            }

            /**
             * Gets the status code of the release.
             *
             * @return the status code, null if not set
             */
            public Integer getCode() {
                return code;
            }

            /**
             * Gets the kubectl output for the release.
             *
             * @return the kubectl cli output, null if not set
             */
            public String getResources() {
                return resources;
            }
        }

        public static final String FIELD_STATUS = "status";

        private Status status;

        /**
         * Creates a new release info.
         *
         * @param status the status info, null returns null
         */
        @JsonCreator
        public Info(@JsonProperty(FIELD_STATUS) Status status) {
            this.status = status;
        }

        /**
         * Gets the status object of the release.
         *
         * @return the status object, null if not set
         */
        public Status getStatus() {
            return status;
        }
    }

    public static final String FIELD_NAME = "name";
    public static final String FIELD_NAMESPACE = "namespace";
    public static final String FIELD_INFO = "info";

    private String name;

    private String namespace;

    private Info info;

    /**
     * Creates a new status response.
     *
     * @param name      the release name (instance id), null returns null
     * @param namespace the release namespace, null returns null
     * @param info      the release info, null returns null
     */
    @JsonCreator
    public ReleaseStatusResponse(@JsonProperty(FIELD_NAME) String name,
        @JsonProperty(FIELD_NAMESPACE) String namespace,
        @JsonProperty(FIELD_INFO) Info info) {
        this.name = name;
        this.namespace = namespace;
        this.info = info;
    }

    /**
     * Gets the name of the release.
     *
     * @return the release name, null if not set
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the namespace of the release.
     *
     * @return the release namespace, null if not set
     */
    public String getNamespace() {
        return namespace;
    }

    /**
     * Gets the Info object of the release.
     *
     * @return the info object, null if not set
     */
    public Info getInfo() {
        return info;
    }
}
