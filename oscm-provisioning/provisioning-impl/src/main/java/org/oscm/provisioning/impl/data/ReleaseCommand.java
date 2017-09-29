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

public interface ReleaseCommand extends Jsonable {

    String FIELD_RELEASE = "release";
    String FIELD_SERVICES = "services";
    String FIELD_FAILURE = "failure";

    public final class UpdateRelease
        implements ReleaseCommand, PersistentEntity.ReplyType<Done> {

        private Release release;

        @JsonCreator
        public UpdateRelease(
            @JsonProperty(FIELD_RELEASE) Release release) {
            this.release = release;
        }

        @JsonProperty(FIELD_RELEASE)
        public Release getRelease() {
            return release;
        }
    }

    public enum DeleteRelease
        implements ReleaseCommand, PersistentEntity.ReplyType<Done> {
        INSTANCE
    }

    public enum InternalInitiateRelease
        implements ReleaseCommand, PersistentEntity.ReplyType<Done> {
        INSTANCE
    }

    public final class InternalConfirmRelease
        implements ReleaseCommand, PersistentEntity.ReplyType<Done> {

        private Map<String, String> services;

        @JsonCreator
        public InternalConfirmRelease(
            @JsonProperty(FIELD_SERVICES) Map<String, String> services) {
            this.services = services;
        }

        @JsonProperty(FIELD_SERVICES)
        public Map<String, String> getServices() {
            return Collections.unmodifiableMap(services);
        }
    }

    public enum InternalDeleteRelease
        implements ReleaseCommand, PersistentEntity.ReplyType<Done> {
        INSTANCE
    }

    public final class InternalFailRelease
        implements ReleaseCommand, PersistentEntity.ReplyType<Done> {

        private Failure failure;

        @JsonCreator
        public InternalFailRelease(
            @JsonProperty(FIELD_FAILURE) Failure failure) {
            this.failure = failure;
        }

        @JsonProperty(FIELD_FAILURE)
        public Failure getFailure() {
            return failure;
        }
    }

    public enum GetRelease
        implements ReleaseCommand,
        PersistentEntity.ReplyType<ProvisioningRelease> {
        INSTANCE
    }

    public enum InternalGetReleaseState
        implements ReleaseCommand,
        PersistentEntity.ReplyType<ReleaseState> {
        INSTANCE
    }
}
