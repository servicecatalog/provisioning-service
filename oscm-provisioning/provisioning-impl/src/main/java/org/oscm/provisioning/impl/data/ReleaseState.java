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
import java.util.*;

@Immutable
public final class ReleaseState extends Identity implements Jsonable {

    public static final String FIELD_RELEASE = "release";
    public static final String FIELD_STATUS = "status";
    public static final String FIELD_FAILURE = "failure";
    public static final String FIELD_INSTANCE = "instance";
    public static final String FIELD_SERVICES = "services";

    private Optional<Release> release;

    private ReleaseStatus status;

    private String instance;

    private Failure failure;

    private Map<String, String> services;

    public ReleaseState(@JsonProperty(FIELD_ID) UUID id,
        @JsonProperty(FIELD_TIMESTAMP) Date timestamp,
        @JsonProperty(FIELD_RELEASE) Optional<Release> release,
        @JsonProperty(FIELD_STATUS) ReleaseStatus status,
        @JsonProperty(FIELD_INSTANCE) String instance,
        @JsonProperty(FIELD_FAILURE) Failure failure,
        @JsonProperty(FIELD_SERVICES) Map<String, String> services) {
        super(id, timestamp);
        this.release = release;
        this.status = status;
        this.instance = instance;
        this.failure = failure;
        this.services = services;
    }

    @JsonProperty(FIELD_RELEASE)
    public Optional<Release> getRelease() {
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

    @JsonProperty(FIELD_SERVICES)
    public Map<String, String> getServices() {
        return Collections.unmodifiableMap(services);
    }

    public static ReleaseState none() {
        return new ReleaseState(null, null, Optional.empty(),
            ReleaseStatus.NONE, null,
            null, null);
    }

    public ReleaseState installing(ReleaseEvent.InstallingRelease event) {
        return new ReleaseState(event.getId(), event.getTimestamp(),
            Optional.of(event.getRelease()), ReleaseStatus.INSTALLING,
            event.getInstance(), getFailure(), getServices());
    }

    public ReleaseState failedInstall(ReleaseEvent.FailedReleaseInstall event) {
        return new ReleaseState(event.getId(), event.getTimestamp(),
            Optional.of(event.getRelease()), ReleaseStatus.INSTALLING,
            instance, failure, services);
    }

    public ProvisioningRelease getAsAPI() {
        if (release.isPresent()) {

            ProvisioningRelease.Status pStatus = null;
            switch (status) {
            case INSTALLING:
            case UPDATING:
            case DELETING:
                pStatus = ProvisioningRelease.Status.PENDING;
                break;
            case FAILED_INSTALL:
            case FAILED_UPD_DEL:
                pStatus = ProvisioningRelease.Status.FAILED;
                break;
            case DELETED:
                pStatus = ProvisioningRelease.Status.DELETED;
                break;
            case DEPLOYED:
                pStatus = ProvisioningRelease.Status.DEPLOYED;
            }

            return new ProvisioningRelease(getId(),
                getTimestamp(), release.get().getTarget(),
                release.get().getNamespace(),
                new ProvisioningRelease.Template(release.get().getRepository(),
                    release.get().getTemplate(), release.get().getVersion()),
                release.get().getLabels(), release.get().getParameters(),
                pStatus, failure, instance, services);
        } else {
            return null;
        }
    }
}
