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
package eu.seaclouds.platform.planner.core.facade;

import java.util.Map;

public class NodeTemplateFactory {

    public static NodeTemplateFacade createNodeTemplate(Map<String, Object> applicationTemplate,
                                                            Map<String, Object> nodeTemplate){

        String nodeTemplateType = (String) nodeTemplate.get(NodeTemplateFacade.TYPE);
        if(ComputeNodeTemplateFacade.isSupported(nodeTemplateType)){
            return new ComputeNodeTemplateFacade(applicationTemplate, nodeTemplate);
        } else if(PlatformNodeTemplateFacade.isSupported(nodeTemplateType)){
            return new PlatformNodeTemplateFacade(applicationTemplate, nodeTemplate);

        } else {
            return new AbstractNodeTemplateFacade(applicationTemplate, nodeTemplate);
        }

    }

}
