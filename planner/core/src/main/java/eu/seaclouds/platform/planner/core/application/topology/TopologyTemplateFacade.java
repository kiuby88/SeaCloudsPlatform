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
package eu.seaclouds.platform.planner.core.application.topology;


import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.brooklyn.util.collections.MutableList;
import org.apache.brooklyn.util.collections.MutableMap;

import com.google.common.base.Optional;

import eu.seaclouds.platform.planner.core.DamGenerator;
import eu.seaclouds.platform.planner.core.application.topology.modifier.relation.TopologyModifierApplication;
import eu.seaclouds.platform.planner.core.application.topology.nodetemplate.AbstractNodeTemplate;
import eu.seaclouds.platform.planner.core.application.topology.nodetemplate.HostedNodeTemplate;
import eu.seaclouds.platform.planner.core.application.topology.nodetemplate.NodeTemplate;
import eu.seaclouds.platform.planner.core.application.topology.nodetemplate.NodeTemplateFactory;
import eu.seaclouds.platform.planner.core.application.topology.nodetemplate.host.HostNodeTemplate;
import eu.seaclouds.platform.planner.core.application.topology.nodetemplate.host.PaasNodeTemplateFacade;
import eu.seaclouds.platform.planner.core.application.topology.nodetemplate.host.PlatformNodeTemplate;

public class TopologyTemplateFacade {

    private final Map<String, Object> originalAdp;
    private Map<String, Object> originalNodeTemplates;
    private Map<String, Object> topologyTemplate;

    private Map<String, NodeTemplate> nodeTemplates;
    private Map<String, HostNodeTemplate> hostNodeTemplates;
    private Map<String, List<HostedNodeTemplate>> topologyTree;

    public TopologyTemplateFacade(Map<String, Object> adp,
                                  Map<String, NodeTemplate> nodeTemplates,
                                  Map<String, List<HostedNodeTemplate>> topologyTree,
                                  Map<String, HostNodeTemplate> hostNodeTemplates) {

        this.originalAdp = MutableMap.copyOf(adp);
        this.nodeTemplates = nodeTemplates;
        this.topologyTree = topologyTree;
        this.hostNodeTemplates = hostNodeTemplates;
        init();
    }

    @SuppressWarnings("unchecked")
    private void init() {
        initOriginalTopologyTemplate();
        initOriginalNodesTemplates();
    }

    private void initOriginalTopologyTemplate() {
        checkNotNull(originalAdp.get(DamGenerator.TOPOLOGY_TEMPLATE),
                "TopologyTemplate has to a topology_template element");
        topologyTemplate = (Map<String, Object>) originalAdp.get(DamGenerator.TOPOLOGY_TEMPLATE);
    }

    private void initOriginalNodesTemplates() {
        checkNotNull(topologyTemplate.get(DamGenerator.NODE_TEMPLATES),
                "TopologyTemplate has to contain NodeTemplates");
        originalNodeTemplates = (Map<String, Object>) topologyTemplate.get(DamGenerator.NODE_TEMPLATES);
    }

    private void addNodeTemplate(String nodeTemplateId, NodeTemplate nodeTemplate) {
        nodeTemplates.put(nodeTemplateId, nodeTemplate);

        if (nodeTemplate instanceof HostNodeTemplate) {
            addHostNodeTemplate(nodeTemplateId, (HostNodeTemplate) nodeTemplate);
        } else {
            addHostedNodeTemplate(nodeTemplateId, (HostedNodeTemplate) nodeTemplate);
        }
    }

    private void addHostNodeTemplate(String hostNodeTemplateId, HostNodeTemplate hostNodeTemplate) {
        nodeTemplates.put(hostNodeTemplateId, hostNodeTemplate);
        hostNodeTemplates.put(hostNodeTemplateId, hostNodeTemplate);
    }

    private void addHostedNodeTemplate(String hostedNodeTemplateId,
                                       HostedNodeTemplate hostedNodeTemplate) {
        String hostNodeTemplateId = hostedNodeTemplate.getHostNodeName();
        Optional<HostNodeTemplate> foundHostNodeTemplate = findHostNodeTemplate(hostNodeTemplateId);

        if (foundHostNodeTemplate.isPresent()) {
            HostNodeTemplate hostNodeTemplate = foundHostNodeTemplate.get();
            hostedNodeTemplate.setHostNodeTemplate(hostNodeTemplate);

            if (!topologyTree.containsKey(hostNodeTemplateId)) {
                topologyTree.put(hostNodeTemplateId, MutableList.<HostedNodeTemplate>of());
            }
            topologyTree.get(hostNodeTemplateId).add(hostedNodeTemplate);
            nodeTemplates.put(hostedNodeTemplateId, hostedNodeTemplate);
        } else {
            throw new RuntimeException("Host not found " + hostNodeTemplateId + " addin the " +
                    " NodeTemplate " + hostedNodeTemplateId + " in " + this);
        }
    }

    private Optional<HostNodeTemplate> findHostNodeTemplate(String hostNodeTemplateId) {
        return Optional.fromNullable(hostNodeTemplates.get(hostNodeTemplateId));
    }

    public Map<String, Object> getRequiredNodeTypes() {
        Map<String, Object> usedNodeTypes = MutableMap.of();
        for (Map.Entry<String, NodeTemplate> nodeTemplateEntry : nodeTemplates.entrySet()) {
            NodeTemplate nodeTemplate = nodeTemplateEntry.getValue();
            Map<String, Object> nodeType = nodeTemplate.getNodeTypeDefinition();
            if (nodeType != null) {
                usedNodeTypes.put(nodeTemplate.getType(), nodeTemplate.getNodeTypeDefinition());
            }
        }
        return usedNodeTypes;
    }

    public Map<String, Object> getNodeTransformedNodeTemplates() {
        Map<String, Object> transformedNodeTemplates = MutableMap.of();
        for (Map.Entry<String, NodeTemplate> nodeTemplateEntry : nodeTemplates.entrySet()) {
            transformedNodeTemplates
                    .put(nodeTemplateEntry.getKey(), nodeTemplateEntry.getValue().transform());
        }
        return transformedNodeTemplates;
    }

    public Map<String, Object> getLocationPoliciesGroups() {
        Map<String, Object> policiesGroups = MutableMap.of();
        for (Map.Entry<String, HostNodeTemplate> hostEntry : hostNodeTemplates.entrySet()) {
            HostNodeTemplate hostNodeTemplate = hostEntry.getValue();
            policiesGroups.put(hostNodeTemplate.getLocationPolicyGroupName(),
                    hostNodeTemplate.getLocationPolicyGroupValues());
        }
        return policiesGroups;
    }

    @SuppressWarnings("unchecked")
    public void updateNodeTemplates(Map<String, Object> adp) {
        topologyTemplate = (Map<String, Object>) adp.get(DamGenerator.TOPOLOGY_TEMPLATE);
        originalNodeTemplates = (Map<String, Object>) topologyTemplate.get(DamGenerator.NODE_TEMPLATES);
        updateNoExistNodeTemplate(adp);
        updateNodeTemplatesProperties(adp);
    }

    private void updateNoExistNodeTemplate(Map<String, Object> adp) {
        for (Map.Entry<String, Object> newNodeTemplate : originalNodeTemplates.entrySet()) {
            String nodeTemplateId = newNodeTemplate.getKey();
            if (!contained(nodeTemplateId)) {
                NodeTemplate nodeTemplate =
                        NodeTemplateFactory.createNodeTemplate(adp, nodeTemplateId);
                addNodeTemplate(nodeTemplateId, nodeTemplate);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void updateNodeTemplatesProperties(Map<String, Object> adp) {
        for (Map.Entry<String, Object> entry : originalNodeTemplates.entrySet()) {
            String nodeTemplateId = entry.getKey();
            Map<String, Object> nodeTemplateFromAdp = (Map<String, Object>) entry.getValue();
            Map<String, Object> properties =
                    (Map<String, Object>) nodeTemplateFromAdp.get(DamGenerator.PROPERTIES);

            if (contained(nodeTemplateId)) {
                getNodeTemplates().get(nodeTemplateId).updateProperties(properties);
            }
        }
    }

    public void applyModifierApplicator(TopologyModifierApplication applicator) {
        for (Map.Entry<String, NodeTemplate> nodeTemplateEntry : nodeTemplates.entrySet()) {
            applicator.applyModifiers(nodeTemplateEntry.getValue(), this);
        }
    }

    public void joinPlatformNodeTemplates() {
        Map<HostNodeTemplate, List<HostedNodeTemplate>> platformAndChildren =
                extractPlatformTemplatesAndChildren();
        removeHostAndHostedChildren(platformAndChildren.keySet());
        addGeneratedPaasFacades(generatedPaasFacades(platformAndChildren));
    }

    private void addGeneratedPaasFacades(List<PaasNodeTemplateFacade> paasNodeTemplateFacades) {
        for (PaasNodeTemplateFacade paasFacade : paasNodeTemplateFacades) {
            addNodeTemplate(paasFacade.getNodeTemplateId(), paasFacade);
        }
    }

    private Map<HostNodeTemplate, List<HostedNodeTemplate>> extractPlatformTemplatesAndChildren() {
        Map<HostNodeTemplate, List<HostedNodeTemplate>> extracted = MutableMap.of();
        for (Map.Entry<String, HostNodeTemplate> hostEntry : hostNodeTemplates.entrySet()) {
            HostNodeTemplate hostNodeTemplate = hostEntry.getValue();
            if (hostNodeTemplate instanceof PlatformNodeTemplate) {
                extracted.put(hostNodeTemplate, getHostedChildren(hostNodeTemplate));
            }
        }
        return extracted;
    }

    private void removeHostAndHostedChildren(Set<HostNodeTemplate> hostNodeteTemplates) {
        for (HostNodeTemplate hostNodeTemplate : hostNodeteTemplates) {
            List<HostedNodeTemplate> hostedNodeTemplates = topologyTree.get(hostNodeTemplate.getNodeTemplateId());
            nodeTemplates.remove(hostNodeTemplate.getNodeTemplateId());
            hostNodeTemplates.remove(hostNodeTemplate.getNodeTemplateId());
            topologyTree.remove(hostNodeTemplate.getNodeTemplateId());
            removeHostedNodeTemplates(hostedNodeTemplates);
        }
    }

    private void removeHostedNodeTemplates(List<HostedNodeTemplate> hostedNodeTemplates) {
        for (NodeTemplate hostedNodeTemplate : hostedNodeTemplates) {
            nodeTemplates.remove(hostedNodeTemplate.getNodeTemplateId());
        }
    }

    private List<HostedNodeTemplate> getHostedChildren(HostNodeTemplate host) {
        return topologyTree.get(host.getNodeTemplateId());
    }

    private List<PaasNodeTemplateFacade> generatedPaasFacades(Map<HostNodeTemplate, List<HostedNodeTemplate>> hostAndChildren) {
        MutableList<PaasNodeTemplateFacade> paasNodeTemplateFacades = MutableList.of();

        for (Map.Entry<HostNodeTemplate, List<HostedNodeTemplate>> entry : hostAndChildren.entrySet()) {
            paasNodeTemplateFacades.addAll(
                    generatePaasFacadesFromAPlatform(
                            (PlatformNodeTemplate) entry.getKey(),
                            entry.getValue()));
        }
        return paasNodeTemplateFacades;
    }

    private List<PaasNodeTemplateFacade> generatePaasFacadesFromAPlatform(
            PlatformNodeTemplate platformNodeTemplate,
            List<HostedNodeTemplate> childNodeTemplates) {

        List<PaasNodeTemplateFacade> paasNodeTemplateFacades = MutableList.of();
        for (NodeTemplate hostedNode : childNodeTemplates) {
            paasNodeTemplateFacades.add(new PaasNodeTemplateFacade(
                    (AbstractNodeTemplate) hostedNode,
                    platformNodeTemplate));
        }
        return paasNodeTemplateFacades;
    }

    private boolean contained(String nodeTemplateId) {
        return nodeTemplates.containsKey(nodeTemplateId);
    }

    public Object getPropertyValue(String nodeTemplateId, String propertyName) {
        checkNotNull(nodeTemplates.get(nodeTemplateId),
                "Error finding property, nodeTemplate " + nodeTemplateId + "not found");
        return nodeTemplates.get(nodeTemplateId).getPropertyValue(propertyName);
    }

    public String getNodeTypeOf(String nodeTemplateId) {
        checkNotNull(nodeTemplates.get(nodeTemplateId),
                "Error finding nodeTemplate type, " + nodeTemplateId + "not found");
        return nodeTemplates.get(nodeTemplateId).getType();

    }

    public Map<String, NodeTemplate> getNodeTemplates() {
        return nodeTemplates;
    }

    public boolean isDeployedOnIaaS(String nodeId) {
        return nodeTemplates.containsKey(nodeId) && nodeTemplates.get(nodeId).isDeployedOnIaaS();
    }

    public boolean isDeployedOnPaaS(String nodeId) {
        return !isDeployedOnIaaS(nodeId);
    }


}
