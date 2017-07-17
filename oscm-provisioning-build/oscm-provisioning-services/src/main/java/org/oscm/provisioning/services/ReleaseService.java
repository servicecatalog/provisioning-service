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
import org.oscm.common.interfaces.exceptions.ConnectionException;
import org.oscm.common.interfaces.exceptions.ServiceException;
import org.oscm.common.util.ServiceManager;
import org.oscm.provisioning.external.RudderClient;
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

        EventSource<Subscription> source = ServiceManager.getInstance()
                .getEventSource(Entity.SUBSCRIPTION);

        Subscription sub = source.get(release.getId());

        RudderClient client = new RudderClient(sub.getTarget());

        try {
            switch (release.getStatus()) {
            case CREATING:
                client.installRelease(sub, release);
                break;

            case UPDATING:
                client.updateRelease(sub, release);
                break;

            case DELETING:
                client.uninstallRelease(release);
                break;

            default:
                return Collections.emptyList();
            }
        } catch (ConnectionException e) {
            release.setStatus(Status.FAILED);
            release.setFailure(e.getAsFailure());

            return Arrays.asList(release);
        }

        release.setStatus(Status.PENDING);

        return Arrays.asList(release);
    }

    public List<Event> update(Event event) throws ServiceException {

        Release release = Release.class.cast(event);

        if (release.getStatus() != Status.PENDING) {
            return null;
        }

        EventSource<Subscription> source = ServiceManager.getInstance()
                .getEventSource(Entity.SUBSCRIPTION);

        Subscription sub = source.get(release.getId());

        try {
            RudderClient client = new RudderClient(sub.getTarget());

            client.releaseStatus(release);

            if (release.getStatus() != Status.PENDING) {

                return Arrays.asList(release);
            }

        } catch (ConnectionException e) {
            release.setStatus(Status.FAILED);
            release.setFailure(e.getAsFailure());

            return Arrays.asList(release);
        }

        return Collections.emptyList();
    }

    public List<Event> monitor(Event event) throws ServiceException {

        Release release = Release.class.cast(event);

        if (release.getStatus() != Status.DEPLOYED) {
            return null;
        }

        EventSource<Subscription> source = ServiceManager.getInstance()
                .getEventSource(Entity.SUBSCRIPTION);

        Subscription sub = source.get(release.getId());

        try {
            RudderClient client = new RudderClient(sub.getTarget());

            client.releaseStatus(release);

            if (release.getStatus() != Status.DEPLOYED) {

                return Arrays.asList(release);
            }
        } catch (ConnectionException e) {
            release.setStatus(Status.FAILED);
            release.setFailure(e.getAsFailure());

            return Arrays.asList(release);
        }

        return Collections.emptyList();
    }
}
