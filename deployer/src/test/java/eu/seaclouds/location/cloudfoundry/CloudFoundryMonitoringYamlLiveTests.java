/*
* Licensed to the Apache Software Foundation (ASF) under one
* or more contributor license agreements.  See the NOTICE file
* distributed with this work for additional information
* regarding copyright ownership.  The ASF licenses this file
* to you under the Apache License, Version 2.0 (the
* "License"); you may not use this file except in compliance
* with the License.  You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package eu.seaclouds.location.cloudfoundry;

import brooklyn.entity.Application;
import brooklyn.entity.Entity;
import brooklyn.entity.basic.Attributes;
import brooklyn.entity.cloudfoundry.services.CloudFoundryService;
import brooklyn.entity.cloudfoundry.webapp.CloudFoundryWebApp;
import brooklyn.entity.cloudfoundry.webapp.java.JavaCloudFoundryPaasWebApp;
import brooklyn.entity.trait.Startable;
import brooklyn.event.AttributeSensor;
import brooklyn.launcher.camp.SimpleYamlLauncher;
import brooklyn.location.cloudfoundry.PaasLocationConfig;
import brooklyn.test.Asserts;
import brooklyn.util.text.Strings;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

@Test( groups={"Live"} )
public class CloudFoundryMonitoringYamlLiveTests {

    private final String SERVICE_NAME = "test-brooklyn-service-mysql";
    private final String SERVICE_TYPE_ID = "cleardb";
    private final String SERVICE_PLAN = "spark";

    private final String JDBC_SENSOR = "test-brooklyn-monitor-app.credentials.jdbcUrl";
    private final String NAME_SENSOR = "test-brooklyn-monitor-app.credentials.name";
    private final String HOSTNAME_SENSOR = "test-brooklyn-monitor-app.credentials.hostname";
    private final String USERNAME_SENSOR = "test-brooklyn-monitor-app.credentials.username";
    private final String PASSWORD_SENSOR = "test-brooklyn-monitor-app.credentials.password";
    private final String PORT_SENSOR = "test-brooklyn-monitor-app.credentials.port";

    public void deployWebappWithServicesFromYaml(){
        SimpleYamlLauncher launcher = new SimpleYamlLauncher();
        launcher.setShutdownAppsOnExit(false);
        Application app = launcher.launchAppYaml("cf-webapp-db-Monitoring.yaml").getApplication();

        final CloudFoundryService service = (CloudFoundryService)
                findEntityChildByDisplayName(app, "DB HelloWorld Visitors");

        final CloudFoundryWebApp server = (CloudFoundryWebApp)
                findEntityChildByDisplayName(app, "Web AppServer HelloWorld");

        assertNotNull(server);
        assertNotNull(service);

        Asserts.succeedsEventually(new Runnable() {
            public void run() {

                assertNotNull(server.getAttribute(CloudFoundryWebApp.SERVICE_PROCESS_IS_RUNNING));



                assertEquals(server.getAttribute(CloudFoundryWebApp.BOUND_SERVICES).size(), 1);
                assertTrue(server.getAttribute(Startable.SERVICE_UP));
                assertTrue(server.getAttribute(JavaCloudFoundryPaasWebApp
                        .SERVICE_PROCESS_IS_RUNNING));

                assertNotNull(server.getAttribute(Attributes.MAIN_URI));
                assertNotNull(server.getAttribute(JavaCloudFoundryPaasWebApp.ROOT_URL));

                assertEquals(server.getAttribute(JavaCloudFoundryPaasWebApp.DISK),
                        PaasLocationConfig.REQUIRED_DISK.getDefaultValue());
                assertEquals(server.getAttribute(JavaCloudFoundryPaasWebApp.INSTANCES_NUM),
                        PaasLocationConfig.REQUIRED_INSTANCES.getDefaultValue());
                assertEquals(server.getAttribute(JavaCloudFoundryPaasWebApp.MEMORY),
                        PaasLocationConfig.REQUIRED_MEMORY.getDefaultValue());

                assertNotNull(server.getAttribute(CloudFoundryWebApp.VCAP_SERVICES));
                assertFalse(Strings.isBlank(server.getAttribute(CloudFoundryWebApp.VCAP_SERVICES)));

                //service
                assertTrue(service.getAttribute(Startable.SERVICE_UP));
                assertTrue(service.getAttribute(JavaCloudFoundryPaasWebApp
                        .SERVICE_PROCESS_IS_RUNNING));

                assertEquals(service.getAttribute(CloudFoundryService.SERVICE_TYPE_ID),
                        SERVICE_TYPE_ID);
                assertEquals(service.getConfig(CloudFoundryService.PLAN), SERVICE_PLAN);
                assertEquals(service.getConfig(CloudFoundryService.SERVICE_INSTANCE_NAME),
                        SERVICE_NAME);

                assertFalse(Strings.isBlank((String) findSensorValueByName(service, JDBC_SENSOR)));
                assertFalse(Strings.isBlank((String) findSensorValueByName(service, NAME_SENSOR)));
                assertFalse(Strings.isBlank((String) findSensorValueByName(service, HOSTNAME_SENSOR)));
                assertFalse(Strings.isBlank((String) findSensorValueByName(service, USERNAME_SENSOR)));
                assertFalse(Strings.isBlank((String) findSensorValueByName(service, PASSWORD_SENSOR)));
                assertFalse(Strings.isBlank((String) findSensorValueByName(service, PORT_SENSOR)));

                assertNotNull(server.getAttribute(JavaCloudFoundryPaasWebApp.SERVER_PROCESSING_TIME));
                assertNotNull(server.getAttribute(JavaCloudFoundryPaasWebApp.SERVER_REQUESTS));
            }
        });
    }

    private Entity findEntityChildByDisplayName(Application app, String displayName){
        for(Object entity: app.getChildren().toArray())
            if(((Entity)entity).getDisplayName().equals(displayName)){
                return (Entity)entity;
            }
        return null;
    }

    private Object findSensorValueByName(Entity entity, String sensorName){
        AttributeSensor<Object> sensor = findSensorByName(entity, sensorName);
        return entity.getAttribute(sensor);
    }

    @SuppressWarnings("unchecked")
    private AttributeSensor<Object> findSensorByName(Entity entity, String sensorName){
        return (AttributeSensor<Object>) entity.getEntityType().getSensor(sensorName);
    }


}
