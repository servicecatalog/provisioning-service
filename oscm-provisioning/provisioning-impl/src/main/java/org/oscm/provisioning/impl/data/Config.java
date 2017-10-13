/*
 * ****************************************************************************
 *                                                                                
 *    Copyright FUJITSU LIMITED 2017                                           
 *                                                                                                                                
 *    Creation Date: 2017-09-21              
 *                                                                                
 * ****************************************************************************
 */

package org.oscm.provisioning.impl.data;

/**
 * Config class for references to application.conf.
 */
public class Config {

    public static final String RUDDER_USER = "oscm.rudder.user";
    public static final String RUDDER_PASSWORD = "oscm.rudder.password";
    public static final String WATCHDOG_INITIAL_DELAY = "oscm.watchdog.initial.delay";
    public static final String WATCHDOG_EXECUTION_INTERVAL = "oscm.watchdog.execution.interval";
    public static final String WATCHDOG_MONITOR_INTERVAL = "oscm.watchdog.monitor.interval";

    private Config() {
    }
}
