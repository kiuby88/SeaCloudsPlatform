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

package eu.seaclouds.platform.dashboard.rest;

import eu.seaclouds.platform.dashboard.model.SeaCloudsApplicationData;
import eu.seaclouds.platform.dashboard.proxy.DeployerProxy;
import eu.seaclouds.platform.dashboard.utils.TestFixtures;
import eu.seaclouds.platform.dashboard.utils.TestUtils;

import org.testng.annotations.Test;
import org.testng.util.Strings;

import java.util.List;

import static org.testng.Assert.*;

import javax.ws.rs.core.Response;

public class DeployerResourceTest extends AbstractResourceTest<DeployerResource> {

    private final DeployerResource deployerResource = new DeployerResource(getDeployerProxy(), getSlaProxy());

    @Test
    public void testAddApplication() throws Exception {
        SeaCloudsApplicationData application = (SeaCloudsApplicationData) deployerResource.addApplication(getDam()).getEntity();
        assertNotNull(application.getName());
        assertNotNull(application.getToscaDam());
        assertNotNull(application.getAgreementId());
        assertNotNull(application.getDeployerApplicationId());
        assertNotNull(application.getMonitoringRulesIds());
    }

    @Test
    public void testListApplications() throws Exception {
        List<SeaCloudsApplicationData> list = (List<SeaCloudsApplicationData>) deployerResource.listApplications().getEntity();
        assertTrue(list.isEmpty());
        deployerResource.addApplication(getDam());
        deployerResource.addApplication(getDam());
        deployerResource.addApplication(getDam());
        list = (List<SeaCloudsApplicationData>) deployerResource.listApplications().getEntity();
        assertEquals(list.size(), 3);
    }

    @Test
    public void testGetApplication() throws Exception {
        assertNull(deployerResource.getApplication("this-app-doesn't-exist").getEntity());
        SeaCloudsApplicationData application = (SeaCloudsApplicationData) deployerResource.addApplication(getDam()).getEntity();
        assertNotNull(deployerResource.getApplication(application.getSeaCloudsApplicationId()));
    }

    @Test
    public void testRemoveApplication() throws Exception {
        SeaCloudsApplicationData application = (SeaCloudsApplicationData) deployerResource.addApplication(getDam()).getEntity();
        deployerResource.addApplication(getDam());
        deployerResource.addApplication(getDam());

        List<SeaCloudsApplicationData> list = (List<SeaCloudsApplicationData>) deployerResource.listApplications().getEntity();
        assertEquals(list.size(), 3);

        deployerResource.removeApplication(application.getSeaCloudsApplicationId());
        list = (List<SeaCloudsApplicationData>) deployerResource.listApplications().getEntity();
        assertEquals(list.size(), 2);
    }

    @Test
    public void testMigrateApplication() throws Exception {
        String response = (String) deployerResource.migrateEntity(RANDOM_STRING, RANDOM_STRING, getLoc()).getEntity();
        assertFalse(Strings.isNullOrEmpty(response));
    }

    @Test
    public void testMigrateApplicationFalse() throws Exception {

        DeployerProxy d = new DeployerProxy();
        d.setHost("127.0.0.1");
        d.setPort(8081);
        d.setUser("admin");
        d.setPassword("seaclouds");
        assertNotNull(d);

        String loc = TestUtils.getStringFromPath(TestFixtures.NER_TARGET_LOCATION_PATH);

        DeployerResource d2 = new DeployerResource(d, getSlaProxy());
        String response = (String) d2.migrateEntity("zC9z8XeN", "YKq5PjYc", loc).getEntity();
        assertFalse(Strings.isNullOrEmpty(response));

        //SeaCloudsApplicationData application = (SeaCloudsApplicationData) deployerResource.addApplication(getDam()).getEntity();
        //assertNotNull(application.getName());
        //assertNotNull(application.getToscaDam());
        //assertNotNull(application.getAgreementId());
        //assertNotNull(application.getDeployerApplicationId());
        //assertNotNull(application.getMonitoringRulesIds());
    }
}