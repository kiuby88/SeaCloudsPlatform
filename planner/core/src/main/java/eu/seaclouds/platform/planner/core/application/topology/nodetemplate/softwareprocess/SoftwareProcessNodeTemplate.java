package eu.seaclouds.platform.planner.core.application.topology.nodetemplate.softwareprocess;

import java.util.Map;

import eu.seaclouds.platform.planner.core.application.topology.nodetemplate.AbstractHostedNodeTemplate;

public class SoftwareProcessNodeTemplate extends AbstractHostedNodeTemplate {

    public SoftwareProcessNodeTemplate(Map<String, Object> applicationTemplate, String nodeTemplateId) {
        super(applicationTemplate, nodeTemplateId);
    }
}
