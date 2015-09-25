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
package brooklyn.entity.cloudfoundry.webapp.java;

import brooklyn.config.ConfigKey;
import brooklyn.entity.basic.ConfigKeys;
import brooklyn.entity.cloudfoundry.webapp.CloudFoundryWebApp;
import brooklyn.entity.proxying.ImplementedBy;
import brooklyn.entity.trait.Resizable;
import brooklyn.event.AttributeSensor;
import brooklyn.event.basic.MapConfigKey;
import brooklyn.event.basic.Sensors;
import brooklyn.util.flags.SetFromFlag;

/**
 * Java webapp entity for being deployed in a CloudFoundry location.
 */
@ImplementedBy(JavaCloudFoundryPaasWebAppImpl.class)
public interface JavaCloudFoundryPaasWebApp extends CloudFoundryWebApp, Resizable {

    @SetFromFlag("buildpack")
    ConfigKey<String> BUILDPACK = ConfigKeys.newStringConfigKey(
            "cloudFoundryWebApp.application.buildpack", "URL of the required buildpack",
            "https://github.com/cloudfoundry/java-buildpack.git");

    // TODO: I think that java.sysprops are dependent on the buildpack.
    @SetFromFlag("java.sysprops")
    MapConfigKey<String> JAVA_SYSPROPS = new MapConfigKey<String>(String.class,
            "cloudfoundry.java.sysprops",
            "System properties to be passed to the buildpack");

    @SetFromFlag("jm.resource")
    ConfigKey<String> MAIN_MONITOR_RESOURCE = ConfigKeys.newStringConfigKey(
            "app.monitor.resource", "Main resource that will be used to monitor the app",
            "/ GET");

    public static final AttributeSensor<String> MONITOR_URL =
            Sensors.newStringSensor("app.monitor.url", "URL for monitoring the app");

    public static final AttributeSensor<Long> USED_MEMORY =
            Sensors.newLongSensor("app.usedmemory", "Memory used by Application");

    public static final AttributeSensor<Double> DURATION_SUM =
            Sensors.newDoubleSensor("app.resource.durationsum", "Total time used by a resource");

    public static final AttributeSensor<Double> RESOURCE_HITS =
            Sensors.newDoubleSensor("app.resource.hits", "Total time that a resource was used");

    public static final AttributeSensor<Double> RESOURCE_LATENCY =
            Sensors.newDoubleSensor("app.resource.latency", "Latency");


    public static final AttributeSensor<Double> SERVER_PROCESSING_TIME =
            Sensors.newDoubleSensor("app.server.processingtime", "");

    public static final AttributeSensor<Double>  SERVER_REQUESTS=
            Sensors.newDoubleSensor("app.server.requests", "");

    public static final AttributeSensor<Double> SERVER_LATENCY =
            Sensors.newDoubleSensor("app.server.latency", "Latency");


}
