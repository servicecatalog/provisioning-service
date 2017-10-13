/*
 * ****************************************************************************
 *
 *    Copyright FUJITSU LIMITED 2017
 *
 *    Creation Date: 2017-08-02
 *
 * ****************************************************************************
 */

package org.oscm.rudder.api;

import akka.NotUsed;
import com.lightbend.lagom.javadsl.api.CircuitBreaker;
import com.lightbend.lagom.javadsl.api.Descriptor;
import com.lightbend.lagom.javadsl.api.Service;
import com.lightbend.lagom.javadsl.api.ServiceCall;
import com.lightbend.lagom.javadsl.api.transport.Method;
import org.oscm.rudder.api.data.InstallReleaseRequest;
import org.oscm.rudder.api.data.ReleaseStatusResponse;
import org.oscm.rudder.api.data.UpdateReleaseRequest;

import static com.lightbend.lagom.javadsl.api.Service.named;
import static com.lightbend.lagom.javadsl.api.Service.restCall;

/**
 * Service interface describing Rudder endpoints used by the provisioning service.
 */
public interface RudderService extends Service {

    String SERVICE_NAME = "rudder";

    /**
     * Endpoint for installing releases.
     *
     * @return the service call
     */
    ServiceCall<InstallReleaseRequest, NotUsed> install();

    /**
     * Endpoint for updating releases.
     *
     * @return the service call
     */
    ServiceCall<UpdateReleaseRequest, NotUsed> update();

    /**
     * Endpoint for deleting releases.
     *
     * @return the service call
     */
    ServiceCall<NotUsed, NotUsed> delete(String release);

    /**
     * Endpoint for checking the release statuses.
     *
     * @return the service call
     */
    ServiceCall<NotUsed, ReleaseStatusResponse> status(String release,
        String version);

    @Override
    default Descriptor descriptor() {
        return named(SERVICE_NAME).withCalls(
            restCall(Method.POST, "/api/v1/releases", this::install),
            restCall(Method.PUT, "/api/v1/releases", this::update),
            restCall(Method.DELETE, "/api/v1/releases/:release",
                this::delete),
            restCall(Method.GET,
                "/api/v1/releases/:release/:version/status", this::status)
        ).withCircuitBreaker(CircuitBreaker.perNode());
    }
}
