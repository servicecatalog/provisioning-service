/*
 * ****************************************************************************
 *                                                                                
 *    Copyright FUJITSU LIMITED 2017                                           
 *                                                                                                                                
 *    Creation Date: 2017-09-21              
 *                                                                                
 * ****************************************************************************
 */

package org.oscm.provisioning.impl.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lightbend.lagom.serialization.Jsonable;
import org.oscm.lagom.data.Identity;
import org.oscm.provisioning.api.data.ProvisioningRelease;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

/**
 * Data class for the current state of releases.
 */
public final class ReleaseState extends Identity implements Jsonable {

    public static final String FIELD_RELEASE = "release";
    public static final String FIELD_STATUS = "status";
    public static final String FIELD_REASON = "reason";
    public static final String FIELD_INSTANCE = "instanceId";
    public static final String FIELD_ENDPOINTS = "endpoints";

    private Release release;

    private ReleaseStatus status;

    private String instanceId;

    private String reason;

    private Map<String, String> endpoints;

    /**
     * Creates a new release state.
     *
     * @param id         the entities id, null returns null
     * @param timestamp  the entities creation timestamp, null returns null
     * @param release    the release information, null returns null
     * @param status     the release status, null returns null
     * @param instanceId the instance id of the release, null returns null
     * @param reason     the reason for the failure that occurred, null returns null
     * @param endpoints  the available endpoints of the release, null returns empty map
     */
    @JsonCreator
    public ReleaseState(@JsonProperty(FIELD_ID) UUID id,
        @JsonProperty(FIELD_TIMESTAMP) Long timestamp,
        @JsonProperty(FIELD_RELEASE) Release release,
        @JsonProperty(FIELD_STATUS) ReleaseStatus status,
        @JsonProperty(FIELD_INSTANCE) String instanceId,
        @JsonProperty(FIELD_REASON) String reason,
        @JsonProperty(FIELD_ENDPOINTS) Map<String, String> endpoints) {
        super(id, timestamp);
        this.release = release;
        this.status = status;
        this.instanceId = instanceId;
        this.reason = reason;
        this.endpoints = endpoints;
    }

    /**
     * Gets the release information.
     *
     * @return the release info, null if not set
     */
    @JsonProperty(FIELD_RELEASE)
    public Release getRelease() {
        return release;
    }

    /**
     * Gets the status of the release.
     *
     * @return the status, null if not set
     */
    @JsonProperty(FIELD_STATUS)
    public ReleaseStatus getStatus() {
        return status;
    }

    /**
     * Gets the instance id of the release
     *
     * @return the instance id, null if not set
     */
    @JsonProperty(FIELD_INSTANCE)
    public String getInstanceId() {
        return instanceId;
    }

    /**
     * Gets the current failure of the release that occurred.
     *
     * @return the failure, null if not set
     */
    @JsonProperty(FIELD_REASON)
    public String getReason() {
        return reason;
    }

    /**
     * Gets the available endpoints for the release.
     *
     * @return the endpoints
     */
    @JsonProperty(FIELD_ENDPOINTS)
    public Map<String, String> getEndpoints() {
        if (endpoints != null) {
            return Collections.unmodifiableMap(endpoints);
        } else {
            return Collections.emptyMap();
        }
    }

    /**
     * Creates a new initial release state.
     *
     * @return the state
     */
    public static ReleaseState none() {
        return new ReleaseState(null, null, null,
            ReleaseStatus.NONE, null, null, null);
    }

    /**
     * Creates a new release state from the given "installing" event and the current data.
     *
     * @param event the event for installing
     * @return the new state
     */
    public ReleaseState installing(ReleaseEvent.InstallingRelease event) {
        return new ReleaseState(event.getId(), event.getTimestamp(), event.getRelease(),
            ReleaseStatus.INSTALLING, event.getInstanceId(), null, null);
    }

    /**
     * Creates a new release state from the given "updating" event and the current data.
     *
     * @param event the event for updating
     * @return the new state
     */
    public ReleaseState updating(ReleaseEvent.UpdatingRelease event) {
        return new ReleaseState(event.getId(), event.getTimestamp(), event.getRelease(),
            ReleaseStatus.UPDATING, instanceId, null, null);
    }

    /**
     * Creates a new release state from the given "deleting" event and the current data.
     *
     * @param event the event for deleting
     * @return the new state
     */
    public ReleaseState deleting(ReleaseEvent.DeletingRelease event) {
        return new ReleaseState(event.getId(), event.getTimestamp(), release,
            ReleaseStatus.DELETING, instanceId, null, null);
    }

    /**
     * Creates a new release state from the given "pending" event and the current data.
     *
     * @param event the event for pending
     * @return the new state
     */
    public ReleaseState pending(ReleaseEvent.PendingRelease event) {
        return new ReleaseState(event.getId(), event.getTimestamp(), release, ReleaseStatus.PENDING,
            instanceId, null, null);
    }

    /**
     * Creates a new release state from the given "deployed" event and the current data.
     *
     * @param event the event for deployed
     * @return the new state
     */
    public ReleaseState deployed(ReleaseEvent.DeployedRelease event) {
        return new ReleaseState(event.getId(), event.getTimestamp(), release,
            ReleaseStatus.DEPLOYED, instanceId, null, event.getEndpoints());
    }

    /**
     * Creates a new release state from the given "deleted" event and the current data.
     *
     * @param event the event for deleted
     * @return the new state
     */
    public ReleaseState deleted(ReleaseEvent.DeletedRelease event) {
        return new ReleaseState(event.getId(), event.getTimestamp(), release, ReleaseStatus.DELETED,
            instanceId, null, null);
    }

    /**
     * Creates a new release state from the given "failed" event and the current data.
     *
     * @param event the event for failed
     * @return the new state
     */
    public ReleaseState failed(ReleaseEvent.FailedRelease event) {
        return new ReleaseState(event.getId(), event.getTimestamp(), release, ReleaseStatus.FAILED,
            null, event.getReason(), null);
    }

    /**
     * Creates a new release state from the given "error" event and the current data.
     *
     * @param event the event for error
     * @return the new state
     */
    public ReleaseState error(ReleaseEvent.ErrorRelease event) {
        return new ReleaseState(event.getId(), event.getTimestamp(), release, ReleaseStatus.ERROR,
            instanceId, event.getReason(), endpoints);
    }

    /**
     * Gets this release state as public representation.
     *
     * @return the pubic release representation
     */
    public ProvisioningRelease getAsAPI() {
        ProvisioningRelease.Status pStatus = null;
        switch (status) {
        case INSTALLING:
        case UPDATING:
        case DELETING:
        case PENDING:
            pStatus = ProvisioningRelease.Status.PENDING;
            break;
        case FAILED:
        case ERROR:
            pStatus = ProvisioningRelease.Status.FAILED;
            break;
        case DELETED:
            pStatus = ProvisioningRelease.Status.DELETED;
            break;
        case DEPLOYED:
            pStatus = ProvisioningRelease.Status.DEPLOYED;
        }

        if (release != null) {
            return new ProvisioningRelease(getId(),
                getTimestamp(), release.getTarget(),
                release.getNamespace(),
                new ProvisioningRelease.Template(release.getRepository(), release.getTemplate(),
                    release.getVersion()), release.getLabels(), release.getParameters(), pStatus,
                reason, instanceId, endpoints);
        } else {
            return new ProvisioningRelease(getId(),
                getTimestamp(), null, null, null, null, null, pStatus, reason, instanceId,
                endpoints);
        }
    }
}
