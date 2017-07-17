/*******************************************************************************
 *                                                                              
 *  Copyright FUJITSU LIMITED 2017                                           
 *                                                                                                                                 
 *  Creation Date: Jul 6, 2017                                                      
 *                                                                              
 *******************************************************************************/

package org.oscm.provisioning.external;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import org.oscm.common.interfaces.enums.Messages;
import org.oscm.common.interfaces.exceptions.ServiceException;
import org.oscm.common.interfaces.exceptions.ValidationException;
import org.oscm.common.rest.RestClient;
import org.oscm.provisioning.external.data.InstallReleaseRequest;
import org.oscm.provisioning.external.data.ReleaseStatusResponse;
import org.oscm.provisioning.interfaces.data.Release;
import org.oscm.provisioning.interfaces.data.Subscription;
import org.oscm.provisioning.interfaces.enums.Application;

/**
 * @author miethaner
 *
 */
public class RudderClient {

    private static final String PATH_INSTALL = "/api/v%d/releases";
    private static final String PATH_UNINSTALL = "/api/v%d/releases/%s";
    private static final String PATH_UPDATE = "/api/v%d/releases";
    private static final String PATH_STATUS = "/api/v%d/releases/%s/status";

    private static final String REGEX_SERVICE = "v1/Service";
    private static final String REGEX_NEXT = "==>";

    private static final int SERVICE_COLUMNS = 5;
    private static final int SERVICE_COLUMN_NAME = 0;
    private static final int SERVICE_COLUMN_EXT_IP = 2;

    private static final Integer API_VERSION = Integer.valueOf(1);

    private RestClient client;

    public RudderClient(String url) {
        this.client = new RestClient(Application.RUDDER, url,
                MediaType.APPLICATION_JSON_TYPE);
    }

    public void installRelease(Subscription sub, Release release)
            throws ServiceException {

        InstallReleaseRequest request = new InstallReleaseRequest();
        request.setName(release.getInstance());
        request.setNamespace(sub.getNamespace());
        request.setRepository(sub.getTemplate().getRepository());
        request.setChart(sub.getTemplate().getName());
        request.setVersion(sub.getTemplate().getVersion());
        request.setValues(sub.getParameters());

        client.post(String.format(PATH_INSTALL, API_VERSION), request,
                Object.class, Status.OK.getStatusCode());
    }

    public void uninstallRelease(Release release) throws ServiceException {

        client.delete(
                String.format(PATH_UNINSTALL, API_VERSION,
                        release.getInstance()),
                Object.class, Status.OK.getStatusCode());
    }

    public void updateRelease(Subscription sub, Release release)
            throws ServiceException {

        InstallReleaseRequest request = new InstallReleaseRequest();
        request.setName(release.getInstance());
        request.setNamespace(sub.getNamespace());
        request.setRepository(sub.getTemplate().getRepository());
        request.setChart(sub.getTemplate().getName());
        request.setVersion(sub.getTemplate().getVersion());
        request.setValues(sub.getParameters());

        client.put(String.format(PATH_UPDATE, API_VERSION), request,
                Object.class, Status.OK.getStatusCode());
    }

    public void releaseStatus(Release release) throws ServiceException {

        ReleaseStatusResponse response = client.get(
                String.format(PATH_STATUS, API_VERSION, release.getInstance()),
                ReleaseStatusResponse.class, Status.OK.getStatusCode());

        switch (response.getInfo().getStatus().getCode()) {
        case UNKNOWN:
            release.setStatus(Release.Status.PENDING);
            break;
        case SUPERSEDED:
        case FAILED:
            release.setStatus(Release.Status.FAILED);
            break;
        case DELETED:
            release.setStatus(Release.Status.DELETED);
            break;
        case DEPLOYED:
            release.setStatus(Release.Status.DEPLOYED);
            release.setServices(extractServices(response));
            break;
        }
    }

    private Map<String, String> extractServices(ReleaseStatusResponse response)
            throws ServiceException {

        Map<String, String> services = new HashMap<>();

        if (response != null && response.getInfo() != null
                && response.getInfo().getStatus() != null
                && response.getInfo().getStatus().getResources() != null
                && !response.getInfo().getStatus().getResources().isEmpty()) {

            String resources = response.getInfo().getStatus().getResources();

            int begin = resources.indexOf(REGEX_SERVICE);
            int end = resources.indexOf(REGEX_NEXT, begin);

            if (begin < 0) {
                throw new ValidationException(Messages.ERROR_BAD_RESPONSE,
                        null);
            }

            if (end < 0) {
                end = resources.length() - 1;
            }

            String[] words = resources.substring(begin, end).split(" ");

            for (int i = SERVICE_COLUMNS
                    + 1; i < words.length; i += SERVICE_COLUMNS) {
                services.put(words[i + SERVICE_COLUMN_NAME],
                        words[i + SERVICE_COLUMN_EXT_IP]);
            }
        }

        return services;
    }
}
