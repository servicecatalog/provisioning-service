/*******************************************************************************
 *                                                                              
 *  Copyright FUJITSU LIMITED 2017                                           
 *                                                                                                                                 
 *  Creation Date: Jun 27, 2017                                                      
 *                                                                              
 *******************************************************************************/

package org.oscm.provisioning.interfaces.enums;

import org.oscm.common.interfaces.data.Version;
import org.oscm.common.interfaces.keys.ActivityKey;
import org.oscm.common.interfaces.keys.ApplicationKey;
import org.oscm.common.interfaces.keys.EntityKey;

/**
 * Enum for activity keys. Represents all commands and queries that this
 * application can execute or interacts with. Each key defines an input
 * {@link Entity} and an output entity. The frontend and backend will use these
 * references for de-/serialization and validation. Also a version window can be
 * defined for filtering at the frontend.
 */
public enum Activity implements ActivityKey {

    ; //

    private String name;
    private EntityKey inputEntity;
    private EntityKey outputEntity;
    private Type type;

    private Activity(String name, EntityKey inputEntity, EntityKey outputEntity,
            Type type) {
        this.name = name;
        this.inputEntity = inputEntity;
        this.outputEntity = outputEntity;
        this.type = type;
    }

    @Override
    public String getActivityName() {
        return name;
    }

    @Override
    public EntityKey getInputEntity() {
        return inputEntity;
    }

    @Override
    public EntityKey getOutputEntity() {
        return outputEntity;
    }

    @Override
    public ApplicationKey getApplication() {
        return Application.SELF;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public Version getSince() {
        return new Version(1, 0, 0);
    }

    @Override
    public Version getUntil() {
        return Config.LATEST_VERSION;
    }
}
