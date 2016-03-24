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

import com.google.common.collect.ImmutableList;
import com.google.common.io.Resources;
import eu.seaclouds.platform.planner.core.facade.host.ComputeNodeTemplateFacade;
import eu.seaclouds.platform.planner.core.resolver.DeployerTypesResolver;
import org.apache.brooklyn.util.collections.MutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AbstractNodeTemplateFacade implements NodeTemplateFacade {

    static Logger log = LoggerFactory.getLogger(AbstractNodeTemplateFacade.class);

    private static final String REQUIREMENTS = "requirements";
    private static final String INSTANCES_POC = "instancesPOC";
    private static final String HOST = "host";
    private static final String ARTIFACTS = "artifacts";
    private static final String PROPERTIES = "properties";
    private static final String NODE_TYPES = "node_types";
    private static final String TOPOLOGY_TEMPLATE = "topology_template";
    private static final String NODE_TEMPLATES = "node_templates";
    private static final String DERIVED_FROM = "derived_from";


    public static final String BROOKLYN_IAAS_TYPES_MAPPING =
            "mapping/brooklyn-iaas-types-mapping.yaml";
    public static final String BROOKLYN_PAAS_TYPES_MAPPING =
            "mapping/brooklyn-paas-types-mapping.yaml";

    private Map<String, Object> module;
    private final Map<String, Object> applicationTemplate;
    private List<Map<String, Object>> requirements;
    private List<Map<String, Object>> artifacts;
    private Map<String, Object> nodeTypes;
    private DeployerTypesResolver deployerTypesResolver;

    public AbstractNodeTemplateFacade(Map<String, Object> applicationTemplate,
                                      Map<String, Object> nodeTemplate) {
        this.applicationTemplate = applicationTemplate;
        this.module = nodeTemplate;
        init();
        customize();
    }

    public Map<String, Object> transform() {
        Map<String, Object> nodeTemplate = MutableMap.of();
        if (!requirements.isEmpty()) {
            nodeTemplate.put(REQUIREMENTS, requirements);
        }
        Map<String, Object> transformedProperties = addArtifactsAsProperties();
        nodeTemplate.put(PROPERTIES, transformedProperties);
        nodeTemplate.put(TYPE, getType());

        return nodeTemplate;
    }

    private void init() {
        initRequirements();
        initArtifacts();
        initNodeTypes();
        initTypeResolver();
    }

    private void customize() {
        customizeRequirements();
    }

    private void initNodeTypes() {
        nodeTypes = (Map<String, Object>) applicationTemplate.get(NODE_TYPES);
    }


    private List<Map<String, Object>> initRequirements() {
        requirements = ImmutableList.of();
        if (module.containsKey(REQUIREMENTS)) {
            requirements = (ArrayList<Map<String, Object>>) module.get(REQUIREMENTS);
        }
        return requirements;
    }

    private List<Map<String, Object>> initArtifacts() {
        artifacts = ImmutableList.of();
        if (module.containsKey(ARTIFACTS)) {
            artifacts = (ArrayList<Map<String, Object>>) module.get(ARTIFACTS);
        }
        return artifacts;
    }


    public List<Map<String, Object>> customizeRequirements() {
        for (Map<String, Object> requirement : requirements) {
            cleanInstancesPocEntry(requirement);
        }
        return requirements;
    }

    private void cleanInstancesPocEntry(Map<String, Object> requirement) {
        if (requirement.containsKey(INSTANCES_POC)) {
            requirement.remove(INSTANCES_POC);
        }
    }

    private Map<String, Object> getHostRequirement() {
        for (Map<String, Object> req : requirements) {
            if (req.containsKey(HOST)) {
                return req;
            }
        }
        return null;
    }

    public String getHostNodeName() {
        String result = null;
        Map<String, Object> hostRequirement = getHostRequirement();
        if (hostRequirement != null) {
            result = (String) hostRequirement.get(HOST);
        }
        return result;
    }

    private Map<String, Object> getNodeTemplates() {
        return (Map<String, Object>) getTopologyTemplate().get(NODE_TEMPLATES);
    }

    private Map<String, Object> getTopologyTemplate() {
        return (Map<String, Object>) applicationTemplate.get(TOPOLOGY_TEMPLATE);
    }

    private Map<String, Object> getHostTemplate() {
        return (Map<String, Object>) getNodeTemplates().get(getHostNodeName());
    }

    private Map<String, Object> addArtifactsAsProperties() {
        if (!artifacts.isEmpty()) {
            for (Map<String, Object> artifact : artifacts) {
                artifact.remove(TYPE);
                Set<String> artifactKeys = artifact.keySet();
                if (artifactKeys.size() > 1) {
                    log.error("Artifact element was not found in {}", module);
                    throw new IllegalArgumentException("Malformed artifact. Item was not found");
                }
                String[] keys = artifactKeys.toArray(new String[1]);
                setPropertyToNodeTemplate(keys[0], artifact.get(keys[0]));
            }
        }
        return getProperties();
    }

    private void setPropertyToNodeTemplate(String propertyName, Object propertyValue) {
        getProperties().put(propertyName, propertyValue);
    }

    private Map<String, Object> getProperties() {
        return (Map<String, Object>) module.get(PROPERTIES);
    }

    private String getParentType() {
        String moduleType = getModuleType();

        if (nodeTypes.containsKey(moduleType)) {
            Map<String, Object> type = (HashMap<String, Object>) nodeTypes.get(moduleType);
            return (String) type.get(DERIVED_FROM);
        }
        return moduleType;
    }

    @Override
    public String getType() {
        return deployerTypesResolver.resolveNodeType(getParentType());
    }

    @Override
    public String getModuleType() {
        return (String) module.get(TYPE);
    }

    private void initTypeResolver() {
        if (moduleIsDeployedOnIaaS()) {
            getDeployerIaaSTypeResolver();
        } else {
            getDeployerPaaSTypeResolver();
        }
    }

    private boolean moduleIsDeployedOnIaaS() {
        return isAnIaasType(getModuleType()) || hostDeployedOnIaaS();
    }

    private boolean isAnIaasType(String type) {
        return ComputeNodeTemplateFacade.isSupported(type);
    }

    private boolean hostDeployedOnIaaS() {
        String hostName = getHostNodeName();
        if (hostName != null) {
            String type = (String) getHostTemplate().get(TYPE);
            return isAnIaasType(type);
        }
        return false;
    }

    public void getDeployerIaaSTypeResolver() {
        try {
            if (deployerTypesResolver == null) {
                deployerTypesResolver = new DeployerTypesResolver(Resources
                        .getResource(BROOKLYN_IAAS_TYPES_MAPPING).toURI().toString());
            }
        } catch (Exception e) {
            log.error("TypeResolver can not be build fof IaaS");
            throw new RuntimeException(e);
        }
    }

    public void getDeployerPaaSTypeResolver() {
        try {
            if (deployerTypesResolver == null) {
                deployerTypesResolver = new DeployerTypesResolver(Resources
                        .getResource(BROOKLYN_PAAS_TYPES_MAPPING).toURI().toString());
            }
        } catch (Exception e) {
            log.error("TypeResolver can not be build fof PaaS");
            throw new RuntimeException(e);
        }
    }

    public Map<String, Object> getNodeTypeDefinition() {
        return (Map<String, Object>) deployerTypesResolver.getNodeTypeDefinition(getType());
    }

    protected DeployerTypesResolver getDeployerTypesResolver(){
        return deployerTypesResolver;
    }

}
