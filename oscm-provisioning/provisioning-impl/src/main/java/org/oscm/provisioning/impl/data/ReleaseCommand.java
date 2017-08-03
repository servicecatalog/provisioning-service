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

import akka.Done;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lightbend.lagom.javadsl.persistence.PersistentEntity;
import com.lightbend.lagom.serialization.Jsonable;
import org.oscm.provisioning.api.data.ProvisioningRelease;

import javax.annotation.concurrent.Immutable;

public interface ReleaseCommand extends Jsonable {

    String FIELD_RELEASE = "release";

    @Immutable
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

    public enum GetRelease
        implements ReleaseCommand,
        PersistentEntity.ReplyType<ProvisioningRelease> {
        INSTANCE
    }
}
