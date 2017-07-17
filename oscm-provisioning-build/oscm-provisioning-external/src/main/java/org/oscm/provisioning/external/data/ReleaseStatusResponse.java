/*******************************************************************************
 *                                                                              
 *  Copyright FUJITSU LIMITED 2017                                           
 *                                                                                                                                 
 *  Creation Date: Jul 7, 2017                                                      
 *                                                                              
 *******************************************************************************/

package org.oscm.provisioning.external.data;

import java.util.Date;

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
    public static final String FIELD_CODE = "code";
    public static final String FIELD_DETAILS = "details";
    public static final String FIELD_RESOURCES = "resource";
    public static final String FIELD_NOTES = "notes";
    public static final String FIELD_TYPE_URL = "type_url";
    public static final String FIELD_VALUE = "value";

    public static class Info {

        public static class Status {

            public enum StatusCode {
                UNKNOWN(0), DEPLOYED(1), DELETED(2), SUPERSEDED(3), FAILED(4);

                private int value;

                private StatusCode(int value) {
                    this.value = value;
                }

                public int getValue() {
                    return value;
                }
            }

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
            private StatusCode code;

            @SerializedName(FIELD_DETAILS)
            private Details details;

            @SerializedName(FIELD_RESOURCES)
            private String resources;

            @SerializedName(FIELD_NOTES)
            private String notes;

            public StatusCode getCode() {
                return code;
            }

            public void setCode(StatusCode code) {
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

        @SerializedName(FIELD_STATUS)
        private Status status;

        @SerializedName(FIELD_FIRST_DEPLOYED)
        private Date firstDeployed;

        @SerializedName(FIELD_LAST_DEPLOYED)
        private Date lastDeployed;

        @SerializedName(FIELD_DELETED)
        private Date deleted;

        public Status getStatus() {
            return status;
        }

        public void setStatus(Status status) {
            this.status = status;
        }

        public Date getFirstDeployed() {
            return firstDeployed;
        }

        public void setFirstDeployed(Date firstDeployed) {
            this.firstDeployed = firstDeployed;
        }

        public Date getLastDeployed() {
            return lastDeployed;
        }

        public void setLastDeployed(Date lastDeployed) {
            this.lastDeployed = lastDeployed;
        }

        public Date getDeleted() {
            return deleted;
        }

        public void setDeleted(Date deleted) {
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
