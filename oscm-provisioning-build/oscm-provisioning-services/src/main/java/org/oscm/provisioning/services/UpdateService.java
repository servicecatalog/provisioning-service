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
import org.oscm.provisioning.interfaces.enums.Messages;

/**
 * @author miethaner
 *
 */
public class UpdateService {

    public List<Event> update(Event event) throws ServiceException {

        Release release = Release.class.cast(event);

        // TODO call rudder

        // dummy code
        if (release.getOperation() != Operation.UPDATE) {
            throw new ConnectionException(Messages.ERROR, "");
        }

        return Arrays.asList(release);
    }

    public List<Event> monitor(Event event) throws ServiceException {

        Release release = Release.class.cast(event);

        // TODO call rudder

        // dummy code
        if (release.getOperation() != Operation.UPDATE) {
            throw new ConnectionException(Messages.ERROR, "");
        }

        return Arrays.asList(release);
    }
}
