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

    private static final String INSTANCE_FORMAT = "oscm-%s";

    @SuppressWarnings("unused")
    public List<Event> provision(Event event) throws ServiceException {

        Subscription sub = Subscription.class.cast(event);

        EventSource<Release> source = ServiceManager.getInstance()
                .getEventSource(Entity.RELEASE);

        Release old = source.get(sub.getId());

        if (sub.getOperation() == Operation.UPDATE && old == null) {

            Release release = new Release();
            release.setId(sub.getId());
            release.setTarget(sub.getTarget());
            release.setTemplate(sub.getTemplate());
            release.setParameters(sub.getParameters());
            release.setOperation(Operation.UPDATE);
            release.setStatus(Status.CREATING);
            release.setInstance(String.format(INSTANCE_FORMAT,
                    UUID.randomUUID().toString()));

            return Arrays.asList(release);
        }

        if (sub.getOperation() == Operation.UPDATE && old != null
                && old.getTimestamp().before(sub.getTimestamp())) {

            old.setTemplate(sub.getTemplate());
            old.setParameters(sub.getParameters());
            old.setOperation(Operation.UPDATE);
            old.setStatus(Status.UPDATING);

            return Arrays.asList(old);
        }

        if (sub.getOperation() == Operation.DELETE && old != null
                && (old.getStatus() == Status.DEPLOYED
                        || old.getStatus() == Status.PENDING)) {

            old.setOperation(Operation.UPDATE);
            old.setStatus(Status.DELETING);

            return Arrays.asList(old);
        }

        if (sub.getOperation() == Operation.DELETE && old != null
                && old.getStatus() != Status.DEPLOYED
                && old.getStatus() != Status.PENDING
                && old.getOperation() != Operation.DELETE) {

            old.setOperation(Operation.DELETE);
            old.setStatus(Status.DELETED);

            return Arrays.asList(old);
        }

        return Collections.emptyList();
    }

}
