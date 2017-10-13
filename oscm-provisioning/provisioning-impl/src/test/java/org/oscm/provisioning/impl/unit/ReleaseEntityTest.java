/*
 * ****************************************************************************
 *
 *    Copyright FUJITSU LIMITED 2017
 *
 *    Creation Date: 2017-09-21
 *
 * ****************************************************************************
 */

package org.oscm.provisioning.impl.unit;

import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;
import com.lightbend.lagom.javadsl.testkit.PersistentEntityTestDriver;
import com.lightbend.lagom.javadsl.testkit.PersistentEntityTestDriver.Outcome;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.oscm.provisioning.impl.ReleaseEntity;
import org.oscm.provisioning.impl.data.Release;
import org.oscm.provisioning.impl.data.ReleaseCommand;
import org.oscm.provisioning.impl.data.ReleaseCommand.UpdateRelease;
import org.oscm.provisioning.impl.data.ReleaseEvent;
import org.oscm.provisioning.impl.data.ReleaseState;

import java.util.UUID;

import static org.junit.Assert.assertEquals;

public class ReleaseEntityTest {

    static ActorSystem system;

    @BeforeClass
    public static void setup() {
        system = ActorSystem.create();
    }

    @AfterClass
    public static void tearDown() {
        TestKit.shutdownActorSystem(system);
        system = null;
    }

    @Test
    public void testEntity() {
        String id = UUID.randomUUID().toString();

        PersistentEntityTestDriver<ReleaseCommand, ReleaseEvent, ReleaseState> driver =
            new PersistentEntityTestDriver<>(system, new ReleaseEntity(), id);

        UpdateRelease cmd = new UpdateRelease(new Release("target", "namespace", "repository",
            "template", "version", null, null, null));

        Outcome<ReleaseEvent, ReleaseState> result = driver.run(cmd);
        assertEquals("target", result.state().getRelease().getTarget());
    }
}
