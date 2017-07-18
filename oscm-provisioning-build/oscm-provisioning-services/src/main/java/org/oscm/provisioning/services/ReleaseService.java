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
import org.oscm.common.interfaces.exceptions.ConnectionException;
import org.oscm.common.interfaces.exceptions.ServiceException;
import org.oscm.provisioning.external.RudderClient;
import org.oscm.provisioning.interfaces.data.Release;
import org.oscm.provisioning.interfaces.data.Release.Status;

/**
 * @author miethaner
 *
 */
public class ReleaseService {

    public List<Event> execute(Event event) throws ServiceException {

        Release release = Release.class.cast(event);

        RudderClient client = new RudderClient(release.getTarget());

        try {
            switch (release.getStatus()) {
            case CREATING:
                client.installRelease(release);
                break;

            case UPDATING:
                client.updateRelease(release);
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

        try {
            RudderClient client = new RudderClient(release.getTarget());

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

        try {
            RudderClient client = new RudderClient(release.getTarget());

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
