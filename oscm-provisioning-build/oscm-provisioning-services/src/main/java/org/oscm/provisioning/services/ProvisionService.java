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
import org.oscm.common.interfaces.exceptions.ServiceException;
import org.oscm.provisioning.external.RudderClient;
import org.oscm.provisioning.external.data.InstallReleaseRequest;
import org.oscm.provisioning.interfaces.data.Release;
import org.oscm.provisioning.interfaces.data.Release.Status;
import org.oscm.provisioning.interfaces.data.Subscription;

/**
 * @author miethaner
 *
 */
public class ProvisionService {

    public List<Event> provision(Event event) throws ServiceException {

        Subscription sub = Subscription.class.cast(event);

        RudderClient client = new RudderClient(sub.getTarget());

        InstallReleaseRequest request = new InstallReleaseRequest();
        request.setName(sub.getId().toString());
        request.setNamespace(sub.getNamespace());
        request.setRepository(sub.getTemplate().getRepository());
        request.setChart(sub.getTemplate().getName());
        request.setVersion(sub.getTemplate().getVersion());
        request.setValues(sub.getParameters());

        client.installRelease(request);

        Release release = new Release();

        release.setId(sub.getId());
        release.setETag(sub.getETag());
        release.setOperation(Operation.UPDATE);
        release.setStatus(Status.PENDING);

        return Arrays.asList(release);
    }

}
