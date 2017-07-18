/*******************************************************************************
 *                                                                              
 *  Copyright FUJITSU LIMITED 2017                                           
 *                                                                                                                                 
 *  Creation Date: Jul 7, 2017                                                      
 *                                                                              
 *******************************************************************************/

package org.oscm.provisioning.external.data;

import com.google.gson.annotations.SerializedName;

/**
 * @author miethaner
 *
 */
public class ReleaseStatusResponse {

    public static final String FIELD_NAME = "name";
    public static final String FIELD_NAMESPACE = "namespace";
    public static final String FIELD_INFO = "info";
    public static final String FIELD_STATUS = "status";
    public static final String FIELD_FIRST_DEPLOYED = "first_deployed";
    public static final String FIELD_LAST_DEPLOYED = "last_deployed";
    public static final String FIELD_DELETED = "deleted";
    public static final String FIELD_SECONDS = "seconds";
    public static final String FIELD_NANOS = "nanos";
    public static final String FIELD_CODE = "code";
    public static final String FIELD_DETAILS = "details";
    public static final String FIELD_RESOURCES = "resource";
    public static final String FIELD_NOTES = "notes";
    public static final String FIELD_TYPE_URL = "type_url";
    public static final String FIELD_VALUE = "value";

    public static final int UNKNOWN = 0;
    public static final int DEPLOYED = 1;
    public static final int DELETED = 2;
    public static final int SUPERSEDED = 3;
    public static final int FAILED = 4;

    public static class Info {

        public static class Status {

            public static class Details {

                @SerializedName(FIELD_TYPE_URL)
                private String typeUrl;

                @SerializedName(FIELD_VALUE)
                private Byte[] value;

                public String getTypeUrl() {
                    return typeUrl;
                }

                public void setTypeUrl(String typeUrl) {
                    this.typeUrl = typeUrl;
                }

                public Byte[] getValue() {
                    return value;
                }

                public void setValue(Byte[] value) {
                    this.value = value;
                }
            }

            @SerializedName(FIELD_CODE)
            private Integer code;

            @SerializedName(FIELD_DETAILS)
            private Details details;

            @SerializedName(FIELD_RESOURCES)
            private String resources;

            @SerializedName(FIELD_NOTES)
            private String notes;

            public Integer getCode() {
                return code;
            }

            public void setCode(Integer code) {
                this.code = code;
            }

            public Details getDetails() {
                return details;
            }

            public void setDetails(Details details) {
                this.details = details;
            }

            public String getResources() {
                return resources;
            }

            public void setResources(String resources) {
                this.resources = resources;
            }

            public String getNotes() {
                return notes;
            }

            public void setNotes(String notes) {
                this.notes = notes;
            }
        }

        public static class Timestamp {

            @SerializedName(FIELD_SECONDS)
            private Long seconds;

            @SerializedName(FIELD_NANOS)
            private Integer nanos;

            public Long getSeconds() {
                return seconds;
            }

            public void setSeconds(Long seconds) {
                this.seconds = seconds;
            }

            public Integer getNanos() {
                return nanos;
            }

            public void setNanos(Integer nanos) {
                this.nanos = nanos;
            }
        }

        @SerializedName(FIELD_STATUS)
        private Status status;

        @SerializedName(FIELD_FIRST_DEPLOYED)
        private Timestamp firstDeployed;

        @SerializedName(FIELD_LAST_DEPLOYED)
        private Timestamp lastDeployed;

        @SerializedName(FIELD_DELETED)
        private Timestamp deleted;

        public Status getStatus() {
            return status;
        }

        public void setStatus(Status status) {
            this.status = status;
        }

        public Timestamp getFirstDeployed() {
            return firstDeployed;
        }

        public void setFirstDeployed(Timestamp firstDeployed) {
            this.firstDeployed = firstDeployed;
        }

        public Timestamp getLastDeployed() {
            return lastDeployed;
        }

        public void setLastDeployed(Timestamp lastDeployed) {
            this.lastDeployed = lastDeployed;
        }

        public Timestamp getDeleted() {
            return deleted;
        }

        public void setDeleted(Timestamp deleted) {
            this.deleted = deleted;
        }
    }

    @SerializedName(FIELD_NAME)
    private String name;

    @SerializedName(FIELD_NAMESPACE)
    private String namespace;

    @SerializedName(FIELD_INFO)
    private Info info;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public Info getInfo() {
        return info;
    }

    public void setInfo(Info info) {
        this.info = info;
    }
}
