/*******************************************************************************
 *                                                                              
 *  Copyright FUJITSU LIMITED 2017                                           
 *                                                                                                                                 
 *  Creation Date: Jun 27, 2017                                                      
 *                                                                              
 *******************************************************************************/

package org.oscm.provisioning.interfaces.data;

import org.oscm.common.interfaces.data.Event;
import org.oscm.common.interfaces.enums.Messages;
import org.oscm.common.interfaces.exceptions.ServiceException;
import org.oscm.common.interfaces.exceptions.ValidationException;
import org.oscm.common.interfaces.keys.ActivityKey;
import org.oscm.common.util.validators.Validator;

import org.oscm.provisioning.interfaces.enums.Activity;

import com.google.gson.annotations.SerializedName;

/**
 * Sample event class. This represents your data structure at both, frontend and
 * backend side. It is de-/serialized with the gson library from/into JSON. It
 * is responsible for validating content for activities (commands and queries)
 * and can also be versioned by overwriting the corresponding superclasses
 * convertTo and updateFrom.
 */
public class Sample extends Event {

    public static final String FIELD_NAME = "name";
    
    @SerializedName(FIELD_NAME)
    private String name;
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    @Override
    public void validateFor(ActivityKey activityKey) throws ServiceException {
       
        Activity a = (Activity) activityKey;

        switch (a) {
        case SAMPLE_CREATE:
            validateId(false);
            validateETag(false);
            validateContent();
            break;

        case SAMPLE_UPDATE:
            validateId(true);
            validateContent();
            break;

        case SAMPLE_DELETE:
            validateId(true);
            break;

        case SAMPLE_READ:
            validateId(true);
            break;

        case SAMPLE_READ_BY_NAME:
            validateContent();
            break;

        case SAMPLE_READ_ALL:
            break;

        default:
            throw new ValidationException(Messages.ERROR_BAD_PROPERTY, null);
        }
    }
    
    private void validateContent() throws ServiceException {
        if (name == null) {
            throw new ValidationException(
                    Messages.ERROR_MANDATORY_PROPERTY_NOT_PRESENT,
                    FIELD_NAME);
        } else {
            Validator.validateText(FIELD_NAME, name);
        }
    }

}
