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
import java.util.UUID;

import org.oscm.common.interfaces.data.Event;
import org.oscm.common.interfaces.enums.Operation;
import org.oscm.common.interfaces.events.EventSource;
import org.oscm.common.interfaces.exceptions.ServiceException;
import org.oscm.common.util.ServiceManager;
import org.oscm.provisioning.interfaces.data.Release;
import org.oscm.provisioning.interfaces.data.Release.Status;
import org.oscm.provisioning.interfaces.data.Subscription;
import org.oscm.provisioning.interfaces.enums.Entity;

/**
 * @author miethaner
 *
 */
public class SubscriptionService {

    @SuppressWarnings("unused")
    public List<Event> provision(Event event) throws ServiceException {

        Subscription sub = Subscription.class.cast(event);

        EventSource<Release> source = ServiceManager.getInstance()
                .getEventSource(Entity.RELEASE);

        Release old = source.get(sub.getId());

        Release release = new Release();
        release.setId(sub.getId());
        release.setETag(sub.getETag());

        if (sub.getOperation() == Operation.UPDATE && old == null) {

            release.setOperation(Operation.UPDATE);
            release.setStatus(Status.CREATING);
            release.setInstance(UUID.randomUUID().toString());

            return Arrays.asList(release);
        }

        if (sub.getOperation() == Operation.UPDATE && old != null
                && !old.getETag().equals(sub.getETag())) {

            release.setOperation(Operation.UPDATE);
            release.setStatus(Status.UPDATING);

            return Arrays.asList(release);
        }

        if (sub.getOperation() == Operation.DELETE && old != null
                && old.getOperation() != Operation.DELETE) {

            release.setOperation(Operation.UPDATE);
            release.setStatus(Status.DELETING);

            return Arrays.asList(release);
        }

        return Collections.emptyList();
    }

}
