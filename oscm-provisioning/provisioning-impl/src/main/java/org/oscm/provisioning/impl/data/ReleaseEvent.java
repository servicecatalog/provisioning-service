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
import com.lightbend.lagom.javadsl.persistence.AggregateEvent;
import com.lightbend.lagom.javadsl.persistence.AggregateEventShards;
import com.lightbend.lagom.javadsl.persistence.AggregateEventTag;
import com.lightbend.lagom.javadsl.persistence.AggregateEventTagger;
import com.lightbend.lagom.serialization.Jsonable;
import org.oscm.lagom.data.Failure;
import org.oscm.lagom.data.Identity;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

/**
 * Super class for release event classes.
 * <p>
 * Each event class corresponds to an event state from the state
 * machine for releases.
 */
public abstract class ReleaseEvent extends Identity implements Jsonable,
    AggregateEvent<ReleaseEvent> {

    private static final int NUM_SHARDS = 4;
    private static final String TAG_NAME = "release";

    /**
     * Shard tags for these events within the cluster
     */
    public static final AggregateEventShards<ReleaseEvent> TAG = AggregateEventTag
        .sharded(ReleaseEvent.class, TAG_NAME, NUM_SHARDS);

    public static final String FIELD_RELEASE = "release";
    public static final String FIELD_INSTANCE = "instanceId";
    public static final String FIELD_ENDPOINTS = "endpoints";
    public static final String FIELD_FAILURE = "failure";

    /**
     * Creates a new release event.
     *
     * @param id        the entities id
     * @param timestamp the entities creation timestamp
     */
    protected ReleaseEvent(UUID id, Long timestamp) {
        super(id, timestamp);
    }

    @Override
    public AggregateEventTagger<ReleaseEvent> aggregateTag() {
        return TAG;
    }

    /**
     * Event class for the "installing" state
     */
    public static final class InstallingRelease extends ReleaseEvent {

        private Release release;

        private String instanceId;

        /**
         * Creates a new installing event.
         *
         * @param id         the entities id
         * @param timestamp  the entities creation timestamp
         * @param instanceId the instance id of the release
         * @param release    the release information
         */
        @JsonCreator
        public InstallingRelease(@JsonProperty(FIELD_ID) UUID id,
            @JsonProperty(FIELD_TIMESTAMP) Long timestamp,
            @JsonProperty(FIELD_INSTANCE) String instanceId,
            @JsonProperty(FIELD_RELEASE) Release release) {
            super(id, timestamp);
            this.release = release;
            this.instanceId = instanceId;
        }

        /**
         * Gets the release information
         *
         * @return the release info
         */
        @JsonProperty(FIELD_RELEASE)
        public Release getRelease() {
            return release;
        }

        /**
         * Gets the instance id of the release
         *
         * @return the instance id
         */
        @JsonProperty(FIELD_INSTANCE)
        public String getInstanceId() {
            return instanceId;
        }
    }

    /**
     * Event class for the "updating" state
     */
    public static final class UpdatingRelease extends ReleaseEvent {

        private Release release;

        /**
         * Creates a new updating event.
         *
         * @param id        the entities id
         * @param timestamp the entities creation timestamp
         * @param release   the release information
         */
        @JsonCreator
        public UpdatingRelease(@JsonProperty(FIELD_ID) UUID id,
            @JsonProperty(FIELD_TIMESTAMP) Long timestamp,
            @JsonProperty(FIELD_RELEASE) Release release) {
            super(id, timestamp);
            this.release = release;
        }

        /**
         * Gets the release information
         *
         * @return the release info
         */
        @JsonProperty(FIELD_RELEASE)
        public Release getRelease() {
            return release;
        }
    }

    /**
     * Event class for the "deleting" state
     */
    public static final class DeletingRelease extends ReleaseEvent {

        /**
         * Creates a new deleting event.
         *
         * @param id        the entities id
         * @param timestamp the entities creation timestamp
         */
        @JsonCreator
        public DeletingRelease(@JsonProperty(FIELD_ID) UUID id,
            @JsonProperty(FIELD_TIMESTAMP) Long timestamp) {
            super(id, timestamp);
        }
    }

    /**
     * Event class for the "pending" state
     */
    public static final class PendingRelease extends ReleaseEvent {

        /**
         * Creates a new pending event.
         *
         * @param id        the entities id
         * @param timestamp the entities creation timestamp
         */
        @JsonCreator
        public PendingRelease(@JsonProperty(FIELD_ID) UUID id,
            @JsonProperty(FIELD_TIMESTAMP) Long timestamp) {
            super(id, timestamp);
        }
    }

    /**
     * Event class for the "deployed" state
     */
    public static final class DeployedRelease extends ReleaseEvent {

        private Map<String, String> endpoints;

        /**
         * Creates a new deployed event.
         *
         * @param id        the entities id
         * @param timestamp the entities creation timestamp
         * @param endpoints the service endpoints of the release
         */
        @JsonCreator
        public DeployedRelease(@JsonProperty(FIELD_ID) UUID id,
            @JsonProperty(FIELD_TIMESTAMP) Long timestamp,
            @JsonProperty(FIELD_ENDPOINTS) Map<String, String> endpoints) {
            super(id, timestamp);
            this.endpoints = endpoints;
        }

        /**
         * Gets the service endpoints of the release.
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
    }

    /**
     * Event class for the "deleted" state
     */
    public static final class DeletedRelease extends ReleaseEvent {

        /**
         * Creates a new deleted event.
         *
         * @param id        the entities id
         * @param timestamp the entities creation timestamp
         */
        @JsonCreator
        public DeletedRelease(@JsonProperty(FIELD_ID) UUID id,
            @JsonProperty(FIELD_TIMESTAMP) Long timestamp) {
            super(id, timestamp);
        }
    }

    /**
     * Event class for the "failed" state
     */
    public static final class FailedRelease extends ReleaseEvent {

        private Failure failure;

        /**
         * Creates a new failed event.
         *
         * @param id        the entities id
         * @param timestamp the entities creation timestamp
         * @param failure   the occurred failure
         */
        @JsonCreator
        public FailedRelease(@JsonProperty(FIELD_ID) UUID id,
            @JsonProperty(FIELD_TIMESTAMP) Long timestamp,
            @JsonProperty(FIELD_FAILURE) Failure failure) {
            super(id, timestamp);
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
     * Event class for the "error" state
     */
    public static final class ErrorRelease extends ReleaseEvent {

        private Failure failure;

        /**
         * Creates a new error event.
         *
         * @param id        the entities id
         * @param timestamp the entities creation timestamp
         * @param failure   the occurred failure
         */
        @JsonCreator
        public ErrorRelease(@JsonProperty(FIELD_ID) UUID id,
            @JsonProperty(FIELD_TIMESTAMP) Long timestamp,
            @JsonProperty(FIELD_FAILURE) Failure failure) {
            super(id, timestamp);
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
}
