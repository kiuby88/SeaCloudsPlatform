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

package eu.seaclouds.platform.dashboard.proxy;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import java.util.List;
import java.util.UUID;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.MediaType;

import org.apache.brooklyn.rest.domain.ApplicationSummary;
import org.apache.brooklyn.rest.domain.EffectorSummary;
import org.apache.brooklyn.rest.domain.EntitySummary;
import org.apache.brooklyn.rest.domain.SensorSummary;
import org.apache.brooklyn.rest.domain.TaskSummary;
import org.eclipse.jetty.server.Response;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.squareup.okhttp.mockwebserver.MockResponse;

import eu.seaclouds.platform.dashboard.util.ObjectMapperHelpers;
import eu.seaclouds.platform.dashboard.utils.TestFixtures;
import eu.seaclouds.platform.dashboard.utils.TestUtils;

public class DeployerProxyTest extends AbstractProxyTest<DeployerProxy> {
    private final String RANDOM_STRING = UUID.randomUUID().toString();

    @Override
    public DeployerProxy getProxy() {
        return getSupport().getConfiguration().getDeployerProxy();
    }

    @Test
    public void testGetApplication() throws Exception {
        String json = TestUtils.getStringFromPath(TestFixtures.APPLICATION_PATH);

        getMockWebServer().enqueue(new MockResponse()
                        .setBody(json)
                        .setHeader("Accept", MediaType.APPLICATION_JSON)
                        .setHeader("Content-Type", MediaType.APPLICATION_JSON)
        );
        assertEquals(ObjectMapperHelpers.JsonToObject(json, ApplicationSummary.class), getProxy().getApplication(RANDOM_STRING));
    }

    @Test
    public void testRemoveApplication() throws Exception {
        String json = TestUtils.getStringFromPath(TestFixtures.TASK_SUMMARY_DELETE_PATH);

        getMockWebServer().enqueue(new MockResponse()
                        .setBody(json)
                        .setHeader("Accept", MediaType.APPLICATION_JSON)
                        .setHeader("Content-Type", MediaType.APPLICATION_JSON)
        );

        TaskSummary response = getProxy().removeApplication(RANDOM_STRING);

        // TaskSummary doesn't implement equals(), so we are going to check the IDs
        TaskSummary fixture = ObjectMapperHelpers.JsonToObject(json, TaskSummary.class);
        assertEquals(fixture.getId(), response.getId());
    }

    @Test
    public void testDeployApplication() throws Exception {
        String json = TestUtils.getStringFromPath(TestFixtures.TASK_SUMMARY_DEPLOY_PATH);
        String tosca = TestUtils.getStringFromPath(TestFixtures.TOSCA_DAM_PATH);

        getMockWebServer().enqueue(new MockResponse()
                        .setBody(json)
                        .setHeader("Accept", MediaType.APPLICATION_JSON)
                        .setHeader("Content-Type", MediaType.APPLICATION_JSON)
        );
        TaskSummary response = getProxy().deployApplication(tosca);

        // TaskSummary doesn't implement equals(), so we are going to check the IDs
        TaskSummary fixture = ObjectMapperHelpers.JsonToObject(json, TaskSummary.class);
        assertEquals(fixture.getId(), response.getId());
    }

    @Test
    public void testGetEntitiesFromApplication() throws Exception {
        String json = TestUtils.getStringFromPath(TestFixtures.ENTITIES_PATH);

        getMockWebServer().enqueue(new MockResponse()
                        .setBody(json)
                        .setHeader("Accept", MediaType.APPLICATION_JSON)
                        .setHeader("Content-Type", MediaType.APPLICATION_JSON)
        );
        List<EntitySummary> response = getProxy().getEntitiesFromApplication(RANDOM_STRING);

        List<EntitySummary> fixture = ObjectMapperHelpers.JsonToObjectCollection(json, EntitySummary.class);
        assertEquals(fixture, response);
    }

    @Test
    public void testGetEntitySensors() throws Exception {
        String json = TestUtils.getStringFromPath(TestFixtures.SENSORS_SUMMARIES_PATH);

        getMockWebServer().enqueue(new MockResponse()
                        .setBody(json)
                        .setHeader("Accept", MediaType.APPLICATION_JSON)
                        .setHeader("Content-Type", MediaType.APPLICATION_JSON)
        );
        List<SensorSummary> response = getProxy().getEntitySensors(RANDOM_STRING, RANDOM_STRING);

        List<SensorSummary> fixture = ObjectMapperHelpers.JsonToObjectCollection(json, SensorSummary.class);
        assertEquals(fixture, response);
    }

    @Test
    public void testGetEntitySensorsValue() throws Exception {
        getMockWebServer().enqueue(new MockResponse()
                        .setBody("0.7")
                        .setHeader("Accept", MediaType.APPLICATION_JSON)
                        .setHeader("Content-Type", MediaType.APPLICATION_JSON)
        );

        String response = getProxy().getEntitySensorsValue(RANDOM_STRING, RANDOM_STRING, RANDOM_STRING);
        assertEquals("0.7", response);
    }

    @Test
    public void testGetApplicationsTree() throws Exception {
        String json = TestUtils.getStringFromPath(TestFixtures.APPLICATION_TREE);

        getMockWebServer().enqueue(new MockResponse()
                        .setBody(json)
                        .setHeader("Accept", MediaType.APPLICATION_JSON)
                        .setHeader("Content-Type", MediaType.APPLICATION_JSON)
        );
        JsonNode response = getProxy().getApplicationsTree();

        JsonNode fixture = ObjectMapperHelpers.JsonToObject(json, JsonNode.class);
        assertEquals(fixture, response);
    }

    @Test
    public void testPostEffector() throws Exception {
        String json = TestUtils.getStringFromPath(TestFixtures.TASK_SUMMARY_MIGRATION_PATH);
        String newLocation = TestUtils.getStringFromPath(TestFixtures.NER_TARGET_LOCATION_PATH);
        getMockWebServer().enqueue(new MockResponse()
                        .setBody(json)
                        .setHeader("Accept", MediaType.APPLICATION_JSON)
                        .setHeader("Content-Type", MediaType.APPLICATION_JSON)
        );
        TaskSummary response = getProxy().postEffector(RANDOM_STRING, RANDOM_STRING, RANDOM_STRING, newLocation);
        TaskSummary fixture = ObjectMapperHelpers.JsonToObject(json, TaskSummary.class);
        assertEquals(fixture.getId(), response.getId());
    }

    @Test(expectedExceptions = NotFoundException.class)
    public void testPostBadEffector() throws Exception {
        String newLocation = TestUtils.getStringFromPath(TestFixtures.NER_TARGET_LOCATION_PATH);
        getMockWebServer().enqueue(new MockResponse().setResponseCode(Response.SC_NOT_FOUND));
        getProxy().postEffector(RANDOM_STRING, RANDOM_STRING, RANDOM_STRING, newLocation);
    }

    @Test
    public void t() throws Exception {
        DeployerProxy d = new DeployerProxy();
        d.setHost("127.0.0.1");
        d.setPort(8081);
        d.setUser("admin");
        d.setPassword("seaclouds");
        assertNotNull(d);

        List<EffectorSummary> a = d.getEffectors("zC9z8XeN", "YKq5PjYc");
        assertNotNull(a);

        TaskSummary aa = d.postEffector("zC9z8XeN", "YKq5PjYc", "migrate", "{\"locationSpec\":\"aws-ec2:us-west-2\"}");
        assertNotNull(aa);

        a = d.getEffectors("zC9z8XeN", "YKq5PjYc");
        assertNotNull(a);

    }
}