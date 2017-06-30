/*******************************************************************************
 *                                                                              
 *  Copyright FUJITSU LIMITED 2017                                           
 *                                                                                                                                 
 *  Creation Date: Jun 30, 2017                                                      
 *                                                                              
 *******************************************************************************/

package org.oscm.provisioning.interfaces.data;

import org.oscm.common.interfaces.data.Event;
import org.oscm.common.interfaces.exceptions.ServiceException;
import org.oscm.common.interfaces.keys.ActivityKey;

/**
 * @author miethaner
 *
 */
public class Subscription extends Event {

    @Override
    public void validateFor(ActivityKey activity) throws ServiceException {
        // nothing to validate
    }

}
