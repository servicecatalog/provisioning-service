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
import com.lightbend.lagom.javadsl.api.Descriptor;
import com.lightbend.lagom.javadsl.api.Service;
import com.lightbend.lagom.javadsl.api.ServiceCall;
import com.lightbend.lagom.javadsl.api.transport.Method;
import org.oscm.rudder.api.data.InstallReleaseRequest;
import org.oscm.rudder.api.data.ReleaseStatusResponse;
import org.oscm.rudder.api.data.UpdateReleaseRequest;

import static com.lightbend.lagom.javadsl.api.Service.named;

public interface RudderService extends Service {

    String SERVICE_NAME = "rudder";

    ServiceCall<InstallReleaseRequest, NotUsed> install();

    ServiceCall<UpdateReleaseRequest, NotUsed> update();

    ServiceCall<NotUsed, NotUsed> delete(String release);

    ServiceCall<NotUsed, ReleaseStatusResponse> status(String release,
        String version);

    @Override
    default Descriptor descriptor() {
        return named(SERVICE_NAME).withCalls(
            Service.restCall(Method.POST, "/api/v1/releases", this::install),
            Service.restCall(Method.PUT, "/api/v1/releases", this::update),
            Service.restCall(Method.DELETE, "/api/v1/releases/:release",
                this::delete),
            Service.restCall(Method.GET,
                "/api/v1/releases/:release/:version/status", this::status)
        );
    }
}
