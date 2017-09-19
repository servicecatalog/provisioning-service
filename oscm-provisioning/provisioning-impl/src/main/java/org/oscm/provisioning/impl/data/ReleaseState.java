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
import com.lightbend.lagom.serialization.Jsonable;
import org.oscm.lagom.data.Failure;
import org.oscm.lagom.data.Identity;
import org.oscm.provisioning.api.data.ProvisioningRelease;

import javax.annotation.concurrent.Immutable;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

@Immutable
public final class ReleaseState extends Identity implements Jsonable {

    public static final String FIELD_RELEASE = "release";
    public static final String FIELD_STATUS = "status";
    public static final String FIELD_FAILURE = "failure";
    public static final String FIELD_INSTANCE = "instance";
    public static final String FIELD_ENDPOINTS = "endpoints";

    private Release release;

    private ReleaseStatus status;

    private String instance;

    private Failure failure;

    private Map<String, String> endpoints;

    public ReleaseState(@JsonProperty(FIELD_ID) UUID id,
        @JsonProperty(FIELD_TIMESTAMP) Long timestamp,
        @JsonProperty(FIELD_RELEASE) Release release,
        @JsonProperty(FIELD_STATUS) ReleaseStatus status,
        @JsonProperty(FIELD_INSTANCE) String instance,
        @JsonProperty(FIELD_FAILURE) Failure failure,
        @JsonProperty(FIELD_ENDPOINTS) Map<String, String> endpoints) {
        super(id, timestamp);
        this.release = release;
        this.status = status;
        this.instance = instance;
        this.failure = failure;
        this.endpoints = endpoints;
    }

    @JsonProperty(FIELD_RELEASE)
    public Release getRelease() {
        return release;
    }

    @JsonProperty(FIELD_STATUS)
    public ReleaseStatus getStatus() {
        return status;
    }

    @JsonProperty(FIELD_INSTANCE)
    public String getInstance() {
        return instance;
    }

    @JsonProperty(FIELD_FAILURE)
    public Failure getFailure() {
        return failure;
    }

    @JsonProperty(FIELD_ENDPOINTS)
    public Map<String, String> getEndpoints() {
        return Collections.unmodifiableMap(endpoints);
    }

    public static ReleaseState none() {
        return new ReleaseState(null, null, null,
            ReleaseStatus.NONE, null, null, null);
    }

    public ReleaseState installing(ReleaseEvent.InstallingRelease event) {
        return new ReleaseState(event.getId(), event.getTimestamp(),
            event.getRelease(), ReleaseStatus.INSTALLING, event.getInstance(),
            null, null);
    }

    public ReleaseState updating(ReleaseEvent.UpdatingRelease event) {
        return new ReleaseState(event.getId(), event.getTimestamp(),
            event.getRelease(), ReleaseStatus.UPDATING, instance, null, null);
    }

    public ReleaseState deleting(ReleaseEvent.DeletingRelease event) {
        return new ReleaseState(event.getId(), event.getTimestamp(),
            release, ReleaseStatus.DELETING, instance, null, null);
    }

    public ReleaseState pending(ReleaseEvent.PendingRelease event) {
        return new ReleaseState(event.getId(), event.getTimestamp(),
            release, ReleaseStatus.PENDING, instance, null, null);
    }

    public ReleaseState deployed(ReleaseEvent.DeployedRelease event) {
        return new ReleaseState(event.getId(), event.getTimestamp(),
            release, ReleaseStatus.DEPLOYED, instance, null,
            event.getEndpoints());
    }

    public ReleaseState deleted(ReleaseEvent.DeletedRelease event) {
        return new ReleaseState(event.getId(), event.getTimestamp(),
            release, ReleaseStatus.DELETED, instance, null, null);
    }

    public ReleaseState failed(ReleaseEvent.FailedRelease event) {
        return new ReleaseState(event.getId(), event.getTimestamp(),
            release, ReleaseStatus.FAILED, null, event.getFailure(),
            null);
    }

    public ReleaseState error(ReleaseEvent.ErrorRelease event) {
        return new ReleaseState(event.getId(), event.getTimestamp(),
            release, ReleaseStatus.ERROR, instance, event.getFailure(),
            endpoints);
    }

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
                new ProvisioningRelease.Template(release.getRepository(),
                    release.getTemplate(), release.getVersion()),
                release.getLabels(), release.getParameters(),
                pStatus, failure, instance, endpoints);
        } else {
            return new ProvisioningRelease(getId(),
                getTimestamp(), null, null, null, null, null, pStatus, failure,
                instance, endpoints);
        }
    }
}
