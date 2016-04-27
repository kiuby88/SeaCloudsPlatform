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

import com.google.common.base.Optional;

import eu.seaclouds.platform.planner.core.DamGenerator;
import eu.seaclouds.platform.planner.core.application.topology.nodetemplate.host.ComputeNodeTemplate;
import eu.seaclouds.platform.planner.core.application.topology.nodetemplate.host.PlatformNodeTemplate;
import eu.seaclouds.platform.planner.core.application.topology.nodetemplate.softwareprocess.DatacollectorNodeTemplate;
import eu.seaclouds.platform.planner.core.application.topology.nodetemplate.softwareprocess.NoScalableSoftwareProcess;
import eu.seaclouds.platform.planner.core.application.topology.nodetemplate.softwareprocess.NoScalableSoftwareProcessNodeTemplate;
import eu.seaclouds.platform.planner.core.application.topology.nodetemplate.softwareprocess.ScalableSoftwareProcess;
import eu.seaclouds.platform.planner.core.application.topology.nodetemplate.softwareprocess.ScalableSoftwareProcessNodeTemplate;

public class NodeTemplateFactory {

    public static ComputeNodeTemplate createComputeNodeTemplate(Map<String, Object> applicationTemplate,
                                                                String nodeTemplateId) {
        return new ComputeNodeTemplate(applicationTemplate, nodeTemplateId);
    }

    public static PlatformNodeTemplate createPlatformNodeTemplate(Map<String, Object> applicationTemplate,
                                                                  String nodeTemplateId) {
        return new PlatformNodeTemplate(applicationTemplate, nodeTemplateId);
    }

    public static DatacollectorNodeTemplate createDatacollectorNodeTemplate(Map<String, Object> applicationTemplate,
                                                                            String nodeTemplateId) {
        return new DatacollectorNodeTemplate(applicationTemplate, nodeTemplateId);
    }

    public static NoScalableSoftwareProcess createNoScalableSoftwareProcessNodeTemplate(Map<String, Object> applicationTemplate,
                                                                              String nodeTemplateId) {
        return new NoScalableSoftwareProcessNodeTemplate(applicationTemplate, nodeTemplateId);
    }

    public static ScalableSoftwareProcess createScalableSoftwareProcessNodeTemplate(Map<String, Object> applicationTemplate,
                                                                              String nodeTemplateId) {
        return new ScalableSoftwareProcessNodeTemplate(applicationTemplate, nodeTemplateId);
    }

    public static boolean isComputeHost(Map<String, Object> module) {
        return ComputeNodeTemplate.isSupported(getModuleType(module));
    }

    public static boolean isPlatformHost(Map<String, Object> module) {
        return PlatformNodeTemplate.isSupported(getModuleType(module));
    }

    public static boolean isDatacollector(Map<String, Object> module) {
        return DatacollectorNodeTemplate.isSupported(getModuleType(module));
    }

    public static boolean isSoftwareProcess(Map<String, Object> module) {
        return !(isComputeHost(module) || isPlatformHost(module) || isDatacollector(module));
    }

    private static String getModuleType(Map<String, Object> module) {
        return (String) module.get(NodeTemplate.TYPE);
    }

    public static boolean isScalableSoftwareProcess(Map<String, Object> module){
        boolean scalable = false;
        Optional<Boolean> foundScalableProperty = findScalableProperty(module);
        if(foundScalableProperty.isPresent()){
            scalable = foundScalableProperty.get() && isSoftwareProcess(module);
        }
        return scalable;
    }

    private static Optional<Boolean> findScalableProperty(Map<String, Object> module){
        Map<String, Object> properties = (Map<String, Object>) module.get(DamGenerator.PROPERTIES);
        return Optional.fromNullable((Boolean)properties.get(ScalableSoftwareProcess.AUTOSCALE_PROPERTY));
    }



}
