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

import akka.Done;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lightbend.lagom.javadsl.persistence.PersistentEntity;
import com.lightbend.lagom.serialization.Jsonable;
import org.oscm.lagom.data.Failure;
import org.oscm.provisioning.api.data.ProvisioningRelease;

import java.util.Collections;
import java.util.Map;

/**
 * Interface for release command classes.
 * <p>
 * Internal commands are only to be used by the scheduler.
 */
public interface ReleaseCommand extends Jsonable {

    String FIELD_RELEASE = "release";
    String FIELD_SERVICES = "endpoints";
    String FIELD_FAILURE = "failure";

    /**
     * Installs or updates the release.
     */
    public final class UpdateRelease
        implements ReleaseCommand, PersistentEntity.ReplyType<Done> {

        private Release release;

        /**
         * Creates a new update release command with the given release.
         *
         * @param release the release information, null returns null
         */
        @JsonCreator
        public UpdateRelease(@JsonProperty(FIELD_RELEASE) Release release) {
            this.release = release;
        }

        /**
         * Gets the release information for this event.
         *
         * @return the release info
         */
        @JsonProperty(FIELD_RELEASE)
        public Release getRelease() {
            return release;
        }
    }

    /**
     * Deletes the release.
     */
    public enum DeleteRelease
        implements ReleaseCommand, PersistentEntity.ReplyType<Done> {
        INSTANCE
    }

    /**
     * Indicates that the release is pending.
     */
    public enum InternalInitiateRelease
        implements ReleaseCommand, PersistentEntity.ReplyType<Done> {
        INSTANCE
    }

    /**
     * Confirms the deployment of the release.
     */
    public final class InternalConfirmRelease
        implements ReleaseCommand, PersistentEntity.ReplyType<Done> {

        private Map<String, String> endpoints;

        /**
         * Creates a new confirm release command with the given service endpoints.
         *
         * @param endpoints the service endpoints
         */
        @JsonCreator
        public InternalConfirmRelease(@JsonProperty(FIELD_SERVICES) Map<String, String> endpoints) {
            this.endpoints = endpoints;
        }

        /**
         * Gets the service endpoints of this event.
         *
         * @return the endpoints
         */
        @JsonProperty(FIELD_SERVICES)
        public Map<String, String> getEndpoints() {
            if (endpoints != null) {
                return Collections.unmodifiableMap(endpoints);
            } else {
                return Collections.emptyMap();
            }
        }
    }

    /**
     * Confirms the deletion of the release.
     */
    public enum InternalDeleteRelease
        implements ReleaseCommand, PersistentEntity.ReplyType<Done> {
        INSTANCE
    }

    /**
     * Indicates an error with the release.
     */
    public final class InternalFailRelease
        implements ReleaseCommand, PersistentEntity.ReplyType<Done> {

        private Failure failure;

        /**
         * Creates a new fail release command with the given failure.
         *
         * @param failure the failure
         */
        @JsonCreator
        public InternalFailRelease(@JsonProperty(FIELD_FAILURE) Failure failure) {
            this.failure = failure;
        }

        /**
         * Gets the occurred failure.
         *
         * @return the failure
         */
        @JsonProperty(FIELD_FAILURE)
        public Failure getFailure() {
            return failure;
        }
    }

    /**
     * Gets the current state of the release as public data type.
     */
    public enum GetRelease
        implements ReleaseCommand, PersistentEntity.ReplyType<ProvisioningRelease> {
        INSTANCE
    }

    /**
     * Gets the current state of the release.
     */
    public enum InternalGetReleaseState
        implements ReleaseCommand, PersistentEntity.ReplyType<ReleaseState> {
        INSTANCE
    }
}
