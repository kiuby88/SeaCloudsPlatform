/*
 *  Copyright 2014 SeaClouds
 *  Contact: SeaClouds
 *
 *      Licensed under the Apache License, Version 2.0 (the "License");
 *      you may not use this file except in compliance with the License.
 *      You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *      Unless required by applicable law or agreed to in writing, software
 *      distributed under the License is distributed on an "AS IS" BASIS,
 *      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *      See the License for the specific language governing permissions and
 *      limitations under the License.
 */

package org.apache.brooklyn.entity.migration;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import org.apache.brooklyn.api.entity.EntitySpec;
import org.apache.brooklyn.api.location.Location;
import org.apache.brooklyn.api.location.LocationSpec;
import org.apache.brooklyn.api.mgmt.LocationManager;
import org.apache.brooklyn.api.mgmt.ManagementContext;
import org.apache.brooklyn.core.effector.Effectors;
import org.apache.brooklyn.core.entity.Attributes;
import org.apache.brooklyn.core.entity.EntityInternal;
import org.apache.brooklyn.core.entity.factory.ApplicationBuilder;
import org.apache.brooklyn.core.entity.trait.Startable;
import org.apache.brooklyn.core.test.entity.TestApplication;
import org.apache.brooklyn.entity.migration.effectors.MigrateEffector;
import org.apache.brooklyn.entity.webapp.JavaWebAppSoftwareProcess;
import org.apache.brooklyn.location.ssh.SshMachineLocation;
import org.apache.brooklyn.test.Asserts;
import org.apache.brooklyn.test.EntityTestUtils;
import org.apache.brooklyn.test.HttpTestUtils;
import org.apache.brooklyn.util.collections.MutableMap;
import org.apache.brooklyn.util.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;

public class MJBossServerTest {
    private static final Logger log = LoggerFactory.getLogger(MJBossServerTest.class);

    private SshMachineLocation loc;
    private ManagementContext managementContext;
    private LocationManager locationManager;
    private TestApplication app;

    @BeforeMethod(alwaysRun = true)
    public void setUp() throws Exception {
        app = ApplicationBuilder.newManagedApp(TestApplication.class);
        managementContext = app.getManagementContext();
        locationManager = managementContext.getLocationManager();
        loc = locationManager.createLocation(LocationSpec.create(SshMachineLocation.class)
                .configure("address", "localhost"));
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() throws Exception {
        log.info("Destroy all {}", new Object[]{this});
        if (app != null) {
            app.stop();
        }
    }

    @Test(groups = {"Live"})
    public void testTomcatServerMigration() {

        final MJBoss7Server vanilla = app.createAndManageChild(
                EntitySpec.create(MJBoss7Server.class)
                        .configure(JavaWebAppSoftwareProcess.OPEN_IPTABLES, true)
                        .configure("war", "http://search.maven.org/remotecontent?filepath=org/apache/brooklyn/example/brooklyn-example-hello-world-sql-webapp/0.8.0-incubating/brooklyn-example-hello-world-sql-webapp-0.8.0-incubating.war"));

        Location awsLocation = managementContext.getLocationRegistry().resolve("aws-ec2:us-west-2");
        app.start(ImmutableList.of(awsLocation));

        Asserts.succeedsEventually(new Runnable() {
            @Override
            public void run() {
                assertTrue(app.getAttribute(Startable.SERVICE_UP));
            }
        });

        String preMigrationUrl = vanilla.getAttribute(JavaWebAppSoftwareProcess.ROOT_URL);
        HttpTestUtils.assertUrlReachable(preMigrationUrl);

        EntityTestUtils.assertAttributeEqualsEventually(vanilla, Attributes.SERVICE_UP, Boolean.TRUE);
        assertNotNull(((EntityInternal) vanilla).getEffector("migrate"));

        Effectors.invocation(MigrateEffector.MIGRATE, MutableMap.of("locationSpec", "aws-ec2:eu-central-1"), vanilla).asTask().blockUntilEnded(Duration.FIVE_MINUTES);
        EntityTestUtils.assertAttributeEqualsEventually(vanilla, Attributes.SERVICE_UP, Boolean.TRUE);

        String postMigrationUrl = vanilla.getAttribute(JavaWebAppSoftwareProcess.ROOT_URL);
        HttpTestUtils.assertUrlReachable(postMigrationUrl);

    }


}