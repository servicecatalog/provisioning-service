/*******************************************************************************
 *                                                                              
 *  Copyright FUJITSU LIMITED 2017                                           
 *                                                                                                                                 
 *  Creation Date: Jun 30, 2017                                                      
 *                                                                              
 *******************************************************************************/

package org.oscm.provisioning.services;

import java.util.Arrays;
import java.util.List;

import org.oscm.common.interfaces.data.Event;
import org.oscm.common.interfaces.enums.Operation;
import org.oscm.common.interfaces.exceptions.ConnectionException;
import org.oscm.common.interfaces.exceptions.ServiceException;
import org.oscm.provisioning.interfaces.data.Release;
import org.oscm.provisioning.interfaces.data.Release.Status;
import org.oscm.provisioning.interfaces.data.Subscription;
import org.oscm.provisioning.interfaces.enums.Messages;

/**
 * @author miethaner
 *
 */
public class ProvisionService {

    public List<Event> provision(Event event) throws ServiceException {

        Subscription sub = Subscription.class.cast(event);

        // TODO call rudder

        // dummy code
        if (sub.getOperation() != Operation.UPDATE) {
            throw new ConnectionException(Messages.ERROR, "");
        }

        String releaseName = "";

        Release release = new Release();

        release.setId(sub.getId());
        release.setETag(sub.getETag());
        release.setOperation(Operation.UPDATE);
        release.setReleaseReference(releaseName);
        release.setStatus(Status.PENDING);

        return Arrays.asList(release);
    }

}
