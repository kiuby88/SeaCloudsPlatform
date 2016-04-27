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
package eu.seaclouds.platform.planner.core.application.topology.nodetemplate;

import java.util.Map;

import eu.seaclouds.platform.planner.core.application.topology.nodetemplate.host.HostNodeTemplate;

public abstract class AbstractHostedNodeTemplate extends AbstractNodeTemplate implements HostedNodeTemplate {

    protected HostNodeTemplate hostNodeTemplate;

    public AbstractHostedNodeTemplate(Map<String, Object> applicationTemplate, String nodeTemplateId) {
        super(applicationTemplate, nodeTemplateId);
    }

    public void setHostNodeTemplate(HostNodeTemplate host) {
        this.hostNodeTemplate = hostNodeTemplate;
    }

    public HostNodeTemplate getHostNodeTemplate() {
        return hostNodeTemplate;
    }


}
