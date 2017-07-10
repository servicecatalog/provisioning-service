/*******************************************************************************
 *                                                                              
 *  Copyright FUJITSU LIMITED 2017                                           
 *                                                                                                                                 
 *  Creation Date: Jun 30, 2017                                                      
 *                                                                              
 *******************************************************************************/

package org.oscm.provisioning.services;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.oscm.common.interfaces.data.Event;
import org.oscm.common.interfaces.events.EventSource;
import org.oscm.common.interfaces.exceptions.ServiceException;
import org.oscm.common.util.ServiceManager;
import org.oscm.provisioning.external.RudderClient;
import org.oscm.provisioning.external.data.ReleaseRequest;
import org.oscm.provisioning.external.data.ReleaseStatusResponse;
import org.oscm.provisioning.interfaces.data.Release;
import org.oscm.provisioning.interfaces.data.Release.Status;
import org.oscm.provisioning.interfaces.data.Subscription;
import org.oscm.provisioning.interfaces.enums.Entity;

/**
 * @author miethaner
 *
 */
public class ReleaseService {

    public List<Event> execute(Event event) throws ServiceException {

        Release release = Release.class.cast(event);

        switch (release.getStatus()) {
        case CREATING:
            installRelease(release);
            break;

        case UPDATING:
            updateRelease(release);
            break;

        case DELETING:
            uninstallRelease(release);
            break;

        default:
            return Collections.emptyList();
        }

        release.setStatus(Status.PENDING);

        return Arrays.asList(release);
    }

    public List<Event> update(Event event) throws ServiceException {

        Release release = Release.class.cast(event);

        if (release.getStatus() != Status.PENDING) {
            return null;
        }

        ReleaseStatusResponse response = releaseStatus(release);

        if (response == null) { // TODO use real response
            release.setStatus(Status.DEPLOYED);

            return Arrays.asList(release);
        }

        return Collections.emptyList();
    }

    public List<Event> monitor(Event event) throws ServiceException {

        Release release = Release.class.cast(event);

        if (release.getStatus() != Status.DEPLOYED) {
            return null;
        }

        ReleaseStatusResponse response = releaseStatus(release);

        if (response == null) { // TODO use real response
            release.setStatus(Status.FAILED);

            return Arrays.asList(release);
        }

        return Collections.emptyList();
    }

    private void installRelease(Release release) throws ServiceException {
        EventSource<Subscription> source = ServiceManager.getInstance()
                .getEventSource(Entity.SUBSCRIPTION);

        Subscription sub = source.get(release.getId());

        RudderClient client = new RudderClient(sub.getTarget());

        ReleaseRequest request = new ReleaseRequest();
        request.setName(sub.getId().toString());
        request.setNamespace(sub.getNamespace());
        request.setRepository(sub.getTemplate().getRepository());
        request.setChart(sub.getTemplate().getName());
        request.setVersion(sub.getTemplate().getVersion());
        request.setValues(sub.getParameters());

        client.installRelease(request);
    }

    private void uninstallRelease(Release release) throws ServiceException {
        EventSource<Subscription> source = ServiceManager.getInstance()
                .getEventSource(Entity.SUBSCRIPTION);

        Subscription sub = source.get(release.getId());

        RudderClient client = new RudderClient(sub.getTarget());

        client.uninstallRelease(release.getInstance());
    }

    private void updateRelease(Release release) throws ServiceException {
        EventSource<Subscription> source = ServiceManager.getInstance()
                .getEventSource(Entity.SUBSCRIPTION);

        Subscription sub = source.get(release.getId());

        RudderClient client = new RudderClient(sub.getTarget());

        ReleaseRequest request = new ReleaseRequest();
        request.setName(sub.getId().toString());
        request.setNamespace(sub.getNamespace());
        request.setRepository(sub.getTemplate().getRepository());
        request.setChart(sub.getTemplate().getName());
        request.setVersion(sub.getTemplate().getVersion());
        request.setValues(sub.getParameters());

        client.updateRelease(request);
    }

    private ReleaseStatusResponse releaseStatus(Release release)
            throws ServiceException {

        EventSource<Subscription> source = ServiceManager.getInstance()
                .getEventSource(Entity.SUBSCRIPTION);

        Subscription sub = source.get(release.getId());

        RudderClient client = new RudderClient(sub.getTarget());

        return client.releaseStatus(release.getInstance());
    }
}
