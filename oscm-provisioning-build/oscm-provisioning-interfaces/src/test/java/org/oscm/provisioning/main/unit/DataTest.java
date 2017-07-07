/*******************************************************************************
 *                                                                              
 *  Copyright FUJITSU LIMITED 2017                                           
 *                                                                                                                                 
 *  Creation Date: Jul 6, 2017                                                      
 *                                                                              
 *******************************************************************************/

package org.oscm.provisioning.main.unit;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.Test;
import org.oscm.common.interfaces.data.Version;
import org.oscm.common.interfaces.enums.Operation;
import org.oscm.common.util.serializer.VersionSerializer;
import org.oscm.provisioning.interfaces.data.Release;
import org.oscm.provisioning.interfaces.data.Release.Status;
import org.oscm.provisioning.interfaces.data.Subscription;
import org.oscm.provisioning.interfaces.data.Subscription.Template;
import org.oscm.provisioning.interfaces.enums.Config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * @author miethaner
 *
 */
public class DataTest {

    @Test
    public void testSubscription() {

        Subscription sub = new Subscription();

        Map<String, String> labels = new HashMap<>();
        labels.put("release", "test");

        Map<String, String> params = new HashMap<>();
        params.put("parameter", "test");

        Template template = new Template();
        template.setName("wordpress");
        template.setRepository("stable");
        template.setVersion("latest");

        sub.setId(UUID.randomUUID());
        sub.setETag(UUID.randomUUID());
        sub.setLabels(labels);
        sub.setOperation(Operation.UPDATE);
        sub.setTarget("http://target.com/context/");
        sub.setNamespace("default");
        sub.setTemplate(template);
        sub.setVersion(new Version(17, 2, 0));
        sub.setParameters(params);

        GsonBuilder builder = new GsonBuilder();
        builder.setPrettyPrinting();
        builder.registerTypeHierarchyAdapter(Version.class,
                new VersionSerializer());
        Gson gson = builder.create();

        System.out.println(gson.toJson(sub));
    }

    @Test
    public void testRelease() {

        Release release = new Release();

        Map<String, String> services = new HashMap<>();
        services.put("endpoint", "127.0.0.1:42");

        release.setId(UUID.randomUUID());
        release.setETag(UUID.randomUUID());
        release.setOperation(Operation.UPDATE);
        release.setStatus(Status.PENDING);
        release.setInstance(release.getId().toString());
        release.setServices(services);
        release.setVersion(Config.LATEST_VERSION);

        GsonBuilder builder = new GsonBuilder();
        builder.setPrettyPrinting();
        builder.registerTypeHierarchyAdapter(Version.class,
                new VersionSerializer());
        Gson gson = builder.create();

        System.out.println(gson.toJson(release));
    }
}
