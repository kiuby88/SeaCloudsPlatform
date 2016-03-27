/**
 * Copyright 2014 SeaClouds
 * Contact: SeaClouds
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.seaclouds.platform.planner.core.template.host;

import eu.seaclouds.platform.planner.core.template.NodeTemplate;

import java.util.Map;

public interface HostNodeTemplate extends NodeTemplate {

    public static final String BROOKLYN_LOCATION = "brooklyn.location";
    public static final String ADD_BROOKLYN_LOCATION_PEFIX = "add_brooklyn_location_";
    public static final String MEMBERS = "members";
    public static final String POLICIES = "policies";

    //TODO: should be connected with policies facades
    public Map<String, Object> getLocationPolicyGroupValues();

    public String getLocationPolicyGroupName();
}
