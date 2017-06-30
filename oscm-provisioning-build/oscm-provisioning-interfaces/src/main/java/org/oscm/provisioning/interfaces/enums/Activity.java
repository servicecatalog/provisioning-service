/*******************************************************************************
 *                                                                              
 *  Copyright FUJITSU LIMITED 2017                                           
 *                                                                                                                                 
 *  Creation Date: Jun 27, 2017                                                      
 *                                                                              
 *******************************************************************************/

package org.oscm.provisioning.interfaces.enums;

import org.oscm.common.interfaces.keys.ActivityKey;
import org.oscm.common.interfaces.keys.ApplicationKey;
import org.oscm.common.interfaces.keys.EntityKey;
import org.oscm.common.interfaces.keys.VersionKey;

/**
 * Enum for activity keys. Represents all commands and queries that this
 * application can execute or interacts with. Each key defines an input
 * {@link Entity} and an output entity. The frontend and backend will use these
 * references for de-/serialization and validation. Also a version window can be
 * defined for filtering at the frontend.
 */
public enum Activity implements ActivityKey {

    SAMPLE_READ("read-sample", Entity.SAMPLE, Entity.SAMPLE, Type.QUERY), //
    SAMPLE_READ_BY_NAME("read-sample-by-name", Entity.SAMPLE, Entity.SAMPLE,
            Type.QUERY), //
    SAMPLE_READ_ALL("read-all-samples", Entity.SAMPLE, Entity.SAMPLE,
            Type.QUERY), //

    SAMPLE_CREATE("create-sample", Entity.SAMPLE, Entity.SAMPLE, Type.COMMAND), //
    SAMPLE_UPDATE("update-sample", Entity.SAMPLE, Entity.SAMPLE, Type.COMMAND), //
    SAMPLE_DELETE("delete-sample", Entity.SAMPLE, Entity.SAMPLE, Type.COMMAND); //

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
    public VersionKey getSince() {
        return Version.V_1_0_0;
    }

    @Override
    public VersionKey getUntil() {
        return Version.LATEST;
    }
}
