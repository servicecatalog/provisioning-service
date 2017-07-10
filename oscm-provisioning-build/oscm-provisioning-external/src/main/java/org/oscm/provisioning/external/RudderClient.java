/*******************************************************************************
 *                                                                              
 *  Copyright FUJITSU LIMITED 2017                                           
 *                                                                                                                                 
 *  Creation Date: Jul 6, 2017                                                      
 *                                                                              
 *******************************************************************************/

package org.oscm.provisioning.external;

import javax.ws.rs.core.Response.Status;

import org.oscm.common.interfaces.exceptions.ServiceException;
import org.oscm.common.rest.RestClient;
import org.oscm.common.rest.provider.SimpleMessageProvider;
import org.oscm.provisioning.external.data.ReleaseRequest;
import org.oscm.provisioning.external.data.ReleaseStatusResponse;

/**
 * @author miethaner
 *
 */
public class RudderClient {

    private static final String PATH_INSTALL = "/api/v%d/releases";
    private static final String PATH_UNINSTALL = "/api/v%d/releases/%s";
    private static final String PATH_UPDATE = "/api/v%d/releases";
    private static final String PATH_STATUS = "/api/v%d/releases/%s/status";

    private static final Integer API_VERSION = new Integer(1);

    private RestClient client;

    public RudderClient(String url) {

        Class<?>[] providers = new Class<?>[] { SimpleMessageProvider.class };
        this.client = new RestClient(url, providers, null, null, 0, "admin",
                "admin123");
    }

    public void installRelease(ReleaseRequest request) throws ServiceException {

        client.post(String.format(PATH_INSTALL, API_VERSION), request,
                Object.class, Status.OK.getStatusCode());
    }

    public void uninstallRelease(String release) throws ServiceException {

        client.delete(String.format(PATH_UNINSTALL, API_VERSION, release),
                Object.class, Status.OK.getStatusCode());
    }

    public void updateRelease(ReleaseRequest request) throws ServiceException {

        client.put(String.format(PATH_UPDATE, API_VERSION), request,
                Object.class, Status.OK.getStatusCode());
    }

    public ReleaseStatusResponse releaseStatus(String release)
            throws ServiceException {

        return client.get(String.format(PATH_STATUS, API_VERSION, release),
                ReleaseStatusResponse.class, Status.OK.getStatusCode());
    }
}
