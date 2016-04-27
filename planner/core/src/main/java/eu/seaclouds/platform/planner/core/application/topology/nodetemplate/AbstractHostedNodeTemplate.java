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
