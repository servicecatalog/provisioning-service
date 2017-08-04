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
 * @author miethaner
 */
public class ReleaseStatusResponse {

    public static final int UNKNOWN = 0;
    public static final int DEPLOYED = 1;
    public static final int DELETED = 2;
    public static final int SUPERSEDED = 3;
    public static final int FAILED = 4;

    public static class Info {

        public static class Status {

            public static class Details {

                public static final String FIELD_TYPE_URL = "type_url";
                public static final String FIELD_VALUE = "value";

                private String typeUrl;

                private Byte[] value;

                @JsonCreator
                public Details(@JsonProperty(FIELD_TYPE_URL) String typeUrl,
                    @JsonProperty(FIELD_VALUE) Byte[] value) {
                    this.typeUrl = typeUrl;
                    this.value = value;
                }

                @JsonProperty(FIELD_TYPE_URL)
                public String getTypeUrl() {
                    return typeUrl;
                }

                @JsonProperty(FIELD_VALUE)
                public Byte[] getValue() {
                    return value;
                }
            }

            public static final String FIELD_CODE = "code";
            public static final String FIELD_DETAILS = "details";
            public static final String FIELD_RESOURCES = "resource";
            public static final String FIELD_NOTES = "notes";

            private Integer code;

            private Details details;

            private String resources;

            private String notes;

            @JsonCreator
            public Status(@JsonProperty(FIELD_CODE) Integer code,
                @JsonProperty(FIELD_DETAILS) Details details,
                @JsonProperty(FIELD_RESOURCES) String resources,
                @JsonProperty(FIELD_NOTES) String notes) {
                this.code = code;
                this.details = details;
                this.resources = resources;
                this.notes = notes;
            }

            @JsonProperty(FIELD_CODE)
            public Integer getCode() {
                return code;
            }

            @JsonProperty(FIELD_DETAILS)
            public Details getDetails() {
                return details;
            }

            @JsonProperty(FIELD_RESOURCES)
            public String getResources() {
                return resources;
            }

            @JsonProperty(FIELD_NOTES)
            public String getNotes() {
                return notes;
            }
        }

        public static class Timestamp {

            public static final String FIELD_SECONDS = "seconds";
            public static final String FIELD_NANOS = "nanos";

            private Long seconds;

            private Integer nanos;

            @JsonCreator
            public Timestamp(@JsonProperty(FIELD_SECONDS) Long seconds,
                @JsonProperty(FIELD_NANOS) Integer nanos) {
                this.seconds = seconds;
                this.nanos = nanos;
            }

            @JsonProperty(FIELD_SECONDS)
            public Long getSeconds() {
                return seconds;
            }

            @JsonProperty(FIELD_NANOS)
            public Integer getNanos() {
                return nanos;
            }
        }

        public static final String FIELD_STATUS = "status";
        public static final String FIELD_FIRST_DEPLOYED = "first_deployed";
        public static final String FIELD_LAST_DEPLOYED = "last_deployed";
        public static final String FIELD_DELETED = "deleted";

        private Status status;

        private Timestamp firstDeployed;

        private Timestamp lastDeployed;

        private Timestamp deleted;

        @JsonCreator
        public Info(@JsonProperty(FIELD_STATUS) Status status,
            @JsonProperty(FIELD_FIRST_DEPLOYED) Timestamp firstDeployed,
            @JsonProperty(FIELD_LAST_DEPLOYED) Timestamp lastDeployed,
            @JsonProperty(FIELD_DELETED) Timestamp deleted) {
            this.status = status;
            this.firstDeployed = firstDeployed;
            this.lastDeployed = lastDeployed;
            this.deleted = deleted;
        }

        @JsonProperty(FIELD_STATUS)
        public Status getStatus() {
            return status;
        }

        @JsonProperty(FIELD_FIRST_DEPLOYED)
        public Timestamp getFirstDeployed() {
            return firstDeployed;
        }

        @JsonProperty(FIELD_LAST_DEPLOYED)
        public Timestamp getLastDeployed() {
            return lastDeployed;
        }

        @JsonProperty(FIELD_DELETED)
        public Timestamp getDeleted() {
            return deleted;
        }
    }

    public static final String FIELD_NAME = "name";
    public static final String FIELD_NAMESPACE = "namespace";
    public static final String FIELD_INFO = "info";

    private String name;

    private String namespace;

    private Info info;

    @JsonCreator
    public ReleaseStatusResponse(@JsonProperty(FIELD_NAME) String name,
        @JsonProperty(FIELD_NAMESPACE) String namespace,
        @JsonProperty(FIELD_INFO) Info info) {
        this.name = name;
        this.namespace = namespace;
        this.info = info;
    }

    @JsonProperty(FIELD_NAME)
    public String getName() {
        return name;
    }

    @JsonProperty(FIELD_NAMESPACE)
    public String getNamespace() {
        return namespace;
    }

    @JsonProperty(FIELD_INFO)
    public Info getInfo() {
        return info;
    }
}
