package eu.seaclouds.platform.planner.core.application.topology;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;
import java.util.Map;

import org.apache.brooklyn.util.collections.MutableList;
import org.apache.brooklyn.util.collections.MutableMap;

import com.google.common.base.Optional;

import eu.seaclouds.platform.planner.core.DamGenerator;
import eu.seaclouds.platform.planner.core.application.topology.nodetemplate.HostedNodeTemplate;
import eu.seaclouds.platform.planner.core.application.topology.nodetemplate.NodeTemplate;
import eu.seaclouds.platform.planner.core.application.topology.nodetemplate.NodeTemplateFactory;
import eu.seaclouds.platform.planner.core.application.topology.nodetemplate.host.HostNodeTemplate;
import eu.seaclouds.platform.planner.core.application.topology.nodetemplate.softwareprocess.SoftwareProcess;

public class TopologyTemplateFactory {


    private MutableMap<String, Object> originalAdp;
    private Map<String, Object> originalTopologyTemplate;
    private Map<String, Object> originalNodeTemplates;

    private Map<String, HostNodeTemplate> hostNodeTemplates;
    private Map<String, HostedNodeTemplate> hostedNodeTemplates;
    private Map<String, List<HostedNodeTemplate>> topologyTree;
    private Map<String, NodeTemplate> nodeTemplates;

    public TopologyTemplateFactory(Map<String, Object> adp) {
        this.originalAdp = MutableMap.copyOf(adp);
        hostedNodeTemplates = MutableMap.of();
        hostNodeTemplates = MutableMap.of();
        topologyTree = MutableMap.of();
        nodeTemplates = MutableMap.of();
        init();
    }

    private void init() {
        initOriginalTopologyTemplate();
        initOriginalNodesTemplates();
    }

    private void initOriginalTopologyTemplate() {
        checkNotNull(originalAdp.get(DamGenerator.TOPOLOGY_TEMPLATE),
                "TopologyTemplate has to a topology_template element");
        originalTopologyTemplate = (Map<String, Object>) originalAdp.get(DamGenerator.TOPOLOGY_TEMPLATE);
    }

    private void initOriginalNodesTemplates() {
        checkNotNull(originalTopologyTemplate.get(DamGenerator.NODE_TEMPLATES),
                "TopologyTemplate has to contain NodeTemplates");
        originalNodeTemplates = (Map<String, Object>) originalTopologyTemplate.get(DamGenerator.NODE_TEMPLATES);
    }

    public TopologyTemplateFacade createTopologyTemplate() {
        createHostNodeTemplates();
        createHosedNodeTemplates();
        createTopologyTree();
        return new TopologyTemplateFacade(originalAdp,
                nodeTemplates,
                topologyTree,
                hostNodeTemplates);
    }

    private void createTopologyTree() {
        for (Map.Entry<String, HostedNodeTemplate> hostedEntry : hostedNodeTemplates.entrySet()) {

            String hostedNodeTemplateId = hostedEntry.getKey();
            HostedNodeTemplate hostedNoteTemplate = hostedEntry.getValue();
            Optional<HostNodeTemplate> hostOptional =
                    findHostNodeTemplate(hostedNoteTemplate.getHostNodeName());

            if (hostOptional.isPresent()) {
                HostNodeTemplate hostNodeTemplate = hostOptional.get();
                hostedNoteTemplate.setHostNodeTemplate(hostNodeTemplate);
                addNodeTemplateToTopologyTree(hostedNoteTemplate.getHostNodeName(), hostedNoteTemplate);
            } else {
                throw new RuntimeException("Host " + hostedNodeTemplateId + "not found in " + this);
            }
        }
    }

    private void addNodeTemplateToTopologyTree(String hostNodeName, HostedNodeTemplate hostedNoteTemplate) {
        if (!topologyTree.containsKey(hostNodeName)) {
            topologyTree.put(hostNodeName, MutableList.of(hostedNoteTemplate));
        } else {
            topologyTree.get(hostNodeName).add(hostedNoteTemplate);
        }
    }

    private void createHostNodeTemplates() {
        for (String nodeTemplateId : originalNodeTemplates.keySet()) {
            Map<String, Object> module =
                    (Map<String, Object>) originalNodeTemplates.get(nodeTemplateId);
            HostNodeTemplate hostNodeTemplate = null;

            if (NodeTemplateFactory.isComputeHost(module)) {
                hostNodeTemplate = NodeTemplateFactory.createComputeNodeTemplate(originalAdp, nodeTemplateId);
            } else if (NodeTemplateFactory.isPlatformHost(module)) {
                hostNodeTemplate = NodeTemplateFactory.createPlatformNodeTemplate(originalAdp, nodeTemplateId);
            }

            if (hostNodeTemplate != null) {
                hostNodeTemplates.put(nodeTemplateId, hostNodeTemplate);
                nodeTemplates.put(nodeTemplateId, hostNodeTemplate);
            }
        }
    }

    private void createHosedNodeTemplates() {
        for (String nodeTemplateId : originalNodeTemplates.keySet()) {
            Map<String, Object> module =
                    (Map<String, Object>) originalNodeTemplates.get(nodeTemplateId);
            HostedNodeTemplate nodeTemplate = null;

            if (NodeTemplateFactory.isSoftwareProcess(module)) {

                nodeTemplate = createSoftwareNodeTemplate(nodeTemplateId, module);

            } else if (NodeTemplateFactory.isDatacollector(module)) {
                nodeTemplate = NodeTemplateFactory.createDatacollectorNodeTemplate(originalAdp, nodeTemplateId);
            }
            if (nodeTemplate != null) {
                hostedNodeTemplates.put(nodeTemplateId, nodeTemplate);
                nodeTemplates.put(nodeTemplateId, nodeTemplate);
            }
        }
    }

    private SoftwareProcess createSoftwareNodeTemplate(String nodeTemplteId, Map<String, Object> module){
        if(NodeTemplateFactory.isScalableSoftwareProcess(module)){
            return NodeTemplateFactory.createScalableSoftwareProcessNodeTemplate(originalAdp, nodeTemplteId);
        } else {
            return NodeTemplateFactory.createNoScalableSoftwareProcessNodeTemplate(originalAdp, nodeTemplteId);
        }
    }

    private Optional<HostNodeTemplate> findHostNodeTemplate(String hostNodeTemplateId) {
        return Optional.fromNullable(hostNodeTemplates.get(hostNodeTemplateId));
    }


}
