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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lightbend.lagom.javadsl.persistence.AggregateEvent;
import com.lightbend.lagom.javadsl.persistence.AggregateEventShards;
import com.lightbend.lagom.javadsl.persistence.AggregateEventTag;
import com.lightbend.lagom.javadsl.persistence.AggregateEventTagger;
import com.lightbend.lagom.serialization.Jsonable;
import org.oscm.lagom.data.Failure;
import org.oscm.lagom.data.Identity;

import javax.annotation.concurrent.Immutable;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

public abstract class ReleaseEvent extends Identity implements Jsonable,
    AggregateEvent<ReleaseEvent> {

    private static final int NUM_SHARDS = 4;
    private static final String TAG_NAME = "provisioning";

    public static final AggregateEventShards<ReleaseEvent> TAG = AggregateEventTag
        .sharded(ReleaseEvent.class, TAG_NAME, NUM_SHARDS);

    public static final String FIELD_RELEASE = "release";
    public static final String FIELD_INSTANCE = "instance";
    public static final String FIELD_SERVICES = "services";
    public static final String FIELD_FAILURE = "failure";

    public ReleaseEvent(UUID id, Long timestamp) {
        super(id, timestamp);
    }

    @Override
    public AggregateEventTagger<ReleaseEvent> aggregateTag() {
        return TAG;
    }

    @Immutable
    public static final class InstallingRelease extends ReleaseEvent {

        private Release release;

        private String instance;

        @JsonCreator
        public InstallingRelease(@JsonProperty(FIELD_ID) UUID id,
            @JsonProperty(FIELD_TIMESTAMP) Long timestamp,
            @JsonProperty(FIELD_INSTANCE) String instance,
            @JsonProperty(FIELD_RELEASE) Release release) {
            super(id, timestamp);
            this.release = release;
            this.instance = instance;
        }

        @JsonProperty(FIELD_RELEASE)
        public Release getRelease() {
            return release;
        }

        @JsonProperty(FIELD_INSTANCE)
        public String getInstance() {
            return instance;
        }
    }

    @Immutable
    public static final class UpdatingRelease extends ReleaseEvent {

        private Release release;

        @JsonCreator
        public UpdatingRelease(@JsonProperty(FIELD_ID) UUID id,
            @JsonProperty(FIELD_TIMESTAMP) Long timestamp,
            @JsonProperty(FIELD_RELEASE) Release release) {
            super(id, timestamp);
            this.release = release;
        }

        @JsonProperty(FIELD_RELEASE)
        public Release getRelease() {
            return release;
        }
    }

    @Immutable
    public static final class DeletingRelease extends ReleaseEvent {

        @JsonCreator
        public DeletingRelease(@JsonProperty(FIELD_ID) UUID id,
            @JsonProperty(FIELD_TIMESTAMP) Long timestamp) {
            super(id, timestamp);
        }
    }

    @Immutable
    public static final class DeployedRelease extends ReleaseEvent {

        private Map<String, String> services;

        @JsonCreator
        public DeployedRelease(@JsonProperty(FIELD_ID) UUID id,
            @JsonProperty(FIELD_TIMESTAMP) Long timestamp,
            @JsonProperty(FIELD_SERVICES)
                Map<String, String> services) {
            super(id, timestamp);
            this.services = services;
        }

        @JsonProperty(FIELD_SERVICES)
        public Map<String, String> getServices() {
            return Collections.unmodifiableMap(services);
        }
    }

    @Immutable
    public static final class DeletedRelease extends ReleaseEvent {

        @JsonCreator
        public DeletedRelease(@JsonProperty(FIELD_ID) UUID id,
            @JsonProperty(FIELD_TIMESTAMP) Long timestamp) {
            super(id, timestamp);
        }
    }

    @Immutable
    public static final class FailedRelease extends ReleaseEvent {

        private Release release;

        private Failure failure;

        @JsonCreator
        public FailedRelease(@JsonProperty(FIELD_ID) UUID id,
            @JsonProperty(FIELD_TIMESTAMP) Long timestamp,
            @JsonProperty(FIELD_RELEASE) Release release,
            @JsonProperty(FIELD_FAILURE) Failure failure) {
            super(id, timestamp);
            this.release = release;
            this.failure = failure;
        }

        @JsonProperty(FIELD_RELEASE)
        public Release getRelease() {
            return release;
        }

        @JsonProperty(FIELD_FAILURE)
        public Failure getFailure() {
            return failure;
        }
    }

    @Immutable
    public static final class ErrorRelease extends ReleaseEvent {

        private Failure failure;

        @JsonCreator
        public ErrorRelease(@JsonProperty(FIELD_ID) UUID id,
            @JsonProperty(FIELD_TIMESTAMP) Long timestamp,
            @JsonProperty(FIELD_FAILURE) Failure failure) {
            super(id, timestamp);
            this.failure = failure;
        }

        @JsonProperty(FIELD_FAILURE)
        public Failure getFailure() {
            return failure;
        }
    }
}