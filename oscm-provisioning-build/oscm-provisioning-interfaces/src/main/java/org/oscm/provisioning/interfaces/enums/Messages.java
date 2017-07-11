/*******************************************************************************
 *                                                                              
 *  Copyright FUJITSU LIMITED 2017                                           
 *                                                                                                                                 
 *  Creation Date: Jun 29, 2017                                                      
 *                                                                              
 *******************************************************************************/

package org.oscm.provisioning.interfaces.enums;

import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.oscm.common.interfaces.keys.MessageKey;

/**
 * Enum for message keys. Represents messages for logs and exceptions for this
 * application.
 */
public enum Messages implements MessageKey {

    DEBUG(10000), //
    INFO(10001), //
    ERROR(10002); //

    private static final String BUNDLE = "org.oscm.provisioning.interfaces.messages.Messages";

    private final int code;

    private Messages(int error) {
        this.code = error;
    }

    @Override
    public Integer getCode() {
        return Integer.valueOf(code);
    }

    @Override
    public String getMessage(String... values) {
        String msg;
        try {
            ResourceBundle bundle = ResourceBundle.getBundle(BUNDLE);
            msg = bundle.getString(Integer.toString(code));
        } catch (MissingResourceException e) {
            throw new RuntimeException(
                    "Unable to find message resource bundle");
        }

        return MessageFormat.format(msg, (Object[]) values);
    }
}
