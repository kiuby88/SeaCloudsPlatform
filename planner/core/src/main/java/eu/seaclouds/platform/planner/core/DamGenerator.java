package eu.seaclouds.platform.planner.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import eu.seaclouds.monitor.monitoringdamgenerator.MonitoringDamGenerator;
import eu.seaclouds.monitor.monitoringdamgenerator.MonitoringInfo;
import it.polimi.tower4clouds.rules.MonitoringRule;
import it.polimi.tower4clouds.rules.MonitoringRules;
import org.apache.brooklyn.util.collections.MutableList;
import org.apache.brooklyn.util.collections.MutableMap;
import org.apache.brooklyn.util.text.Identifiers;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import static com.google.common.base.Preconditions.*;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.regex.Pattern;


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
public class DamGenerator {

    static Logger log = LoggerFactory.getLogger(DamGenerator.class);

    private static final String SLA_GEN_OP = "/seaclouds/templates";
    private static final String SLA_INFO_GROUPNAME = "sla_gen_info";
    private static final String MONITOR_INFO_GROUPNAME = "monitoringInformation";

    private static final String ADD_BROOKLYN_LOCATION = "add_brooklyn_location_";
    private static final String BROOKLYN_LOCATION = "brooklyn.location";
    private static final String LOCATION = "location";
    public static final String REGION = "region";
    public static final String HARDWARE_ID = "hardwareId";
    public static final String TYPE = "type";
    public static final String CLOUD_FOUNDRY = "CloudFoundry";
    public static final String POLICIES = "policies";
    public static final String GROUPS = "groups";
    public static final String HOST = "host";
    public static final String INSTANCES_POC = "instancesPOC";
    public static final String REQUIREMENTS = "requirements";
    public static final String MEMBERS = "members";
    public static final String ID = "id";
    public static final String APPLICATION = "application";
    public static final String TOPOLOGY_TEMPLATE = "topology_template";
    public static final String NODE_TEMPLATES = "node_templates";
    public static final String NODE_TYPES = "node_types";
    public static final String PROPERTIES = "properties";
    private static final String BROOKLYN_TYPES_MAPPING = "mapping/brooklyn-types-mapping.yaml";
    private static final String BROOKLYN_POLICY_TYPE = "brooklyn.location";
    public static final String IMPORTS = "imports";
    public static final String TOSCA_NORMATIVE_TYPES = "tosca-normative-types";
    public static final String TOSCA_NORMATIVE_TYPES_VERSION = "1.0.0.wd06-SNAPSHOT";

    public static final String TEMPLATE_NAME = "template_name";
    public static final String TEMPLATE_NAME_PREFIX = "seaclouds.app.";
    public static final String TEMPLATE_VERSION = "template_version";
    public static final String DEFAULT_TEMPLATE_VERSION = "1.0.0-SNAPSHOT";
    public static final String SEACLOUDS_MONITORING_RULES_ID_POLICY = "seaclouds.policies.monitoringrules";
    public static final String MONITORING_RULES_POLICY_NAME = "monitoringrules.information.policy";
    public static final String SEACLOUDS_DC_TYPE = "seaclouds.nodes.Datacollector";
    public static final String SEACLOUDS_APPLICATION_INFORMATION_POLICY_TYPE = "seaclouds.policies.app.information";
    public static final String SEACLOUDS_APPLICATION_POLICY_NAME = "seaclouds.app.information";
    public static final String SEACLOUDS_NODE_PREFIX = "seaclouds.nodes";


    public static final String SEACLOUDS_APPLICATION_CONFIGURATION =
            "seaclouds.application.configuration";
    public static final String SEACLOUDS_APPLICATION_CONFIGURATION_POLICY =
            "seaclouds.policies.application.configuration";

    public static final String SEACLOUDS_INITIALIZER_POLICY =
            "eu.seaclouds.policy.SeaCloudsInitializerPolicy";

    private static DeployerTypesResolver deployerTypesResolver;
    static Map<String, MonitoringInfo> monitoringInfoByApplication=new HashMap<>();

    public static String generateDam(String adp,
                                     String monitorGenURL,
                                     String monitorGenPort,
                                     String influxdbURL,
                                     String influxdbPort,
                                     String influxdbDatabase,
                                     String influxdbUsername,
                                     String influxdbPassword,
                                     String grafanaEndpoint,
                                     String grafanaUsername,
                                     String grafanaPassword,
                                     String slaEndpoint,
                                     String t4cEndpoint){
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Yaml yml = new Yaml(options);
        Map<String, Object> adpYaml = (Map<String, Object>) yml.load(adp);
        adpYaml = DamGenerator.manageTemplateMetada(adpYaml);
        adpYaml = DamGenerator.translateAPD(adpYaml);

        MonitoringInfo monitoringInfo = generateMonitoringInfo(
                yml.dump(adpYaml),
                monitorGenURL,
                monitorGenPort,
                influxdbURL,
                influxdbPort);
        adpYaml = DamGenerator.addMonitorInfo(monitoringInfo);

        ApplicationMonitorId applicationMonitorId = getApplicationMonitorId(adpYaml, slaEndpoint);;
        adpYaml = DamGenerator.addApplicationInfo(adpYaml, applicationMonitorId, SLA_INFO_GROUPNAME);

        addSeaCloudsPolicy(adpYaml,
                monitoringInfo,
                applicationMonitorId,
                slaEndpoint,
                t4cEndpoint,
                influxdbURL,
                influxdbDatabase,
                influxdbUsername,
                influxdbPassword,
                grafanaEndpoint,
                grafanaUsername,
                grafanaPassword);

        Map groups = (Map) adpYaml.remove(GROUPS);

        addPoliciesTypeIfNotPresent(groups);

        ((Map)adpYaml.get(TOPOLOGY_TEMPLATE)).put(GROUPS, groups);



        String adpStr = yml.dump(adpYaml);
        return adpStr;
    }

    public static ApplicationMonitorId getApplicationMonitorId(Map<String, Object> adpYaml, String slaEndpoint){

        Yaml yml = new Yaml();
        ApplicationMonitorId applicationMonitorId = null;
        String slaInfoResponse = null;
        try {
        slaInfoResponse = new HttpHelper(slaEndpoint).postInBody(SLA_GEN_OP, yml.dump(adpYaml));
        checkNotNull(slaInfoResponse, "Error getting SLA info");
        ObjectMapper mapper = new ObjectMapper();
            applicationMonitorId = mapper.readValue(slaInfoResponse, ApplicationMonitorId.class);
        } catch (IOException e) {
            log.error("Error during agreement template generation, response: {} ", slaInfoResponse);
            e.printStackTrace();
        }
        return  applicationMonitorId;
    }

    public static Map<String, Object> manageTemplateMetada(Map<String, Object> adpYaml){
        if(adpYaml.containsKey(IMPORTS)){
            List<String> imports = (List<String>) adpYaml.get(IMPORTS);
            if(imports != null){
                String importedNormativeTypes=null;
                for(String dependency: imports){
                    if(dependency.contains(TOSCA_NORMATIVE_TYPES)){
                        importedNormativeTypes = dependency;
                    }
                }
                if((importedNormativeTypes!=null)&&(!importedNormativeTypes.equals(TOSCA_NORMATIVE_TYPES+":"+TOSCA_NORMATIVE_TYPES_VERSION))){
                    //TODO: an log war message should be necessary here
                    imports.remove(importedNormativeTypes);
                    imports.add(TOSCA_NORMATIVE_TYPES+":"+TOSCA_NORMATIVE_TYPES_VERSION);
                }
            }
        }

        if(!adpYaml.containsKey(TEMPLATE_NAME)){
            adpYaml.put(TEMPLATE_NAME, TEMPLATE_NAME_PREFIX + Identifiers.makeRandomId(8));
        }

        if(!adpYaml.containsKey(TEMPLATE_VERSION)){
            adpYaml.put(TEMPLATE_VERSION, DEFAULT_TEMPLATE_VERSION);
        }

        return adpYaml;
    }

    public static MonitoringInfo generateMonitoringInfo(String adp,
                                                        String monitorUrl,
                                                        String monitorPort,
                                                        String influxdbUrl,
                                                        String influxdbPort){
        MonitoringInfo monitoringInfo = null;
        MonitoringDamGenerator monDamGen = null;
        try {
            monDamGen = new MonitoringDamGenerator(
                    new URL("http://"+ monitorUrl +":"+ monitorPort +""),
                    new URL("http://"+ influxdbUrl +":"+ influxdbPort +""));
        } catch (MalformedURLException e) {
            log.error(e.getMessage());
        }
        monitoringInfo = monDamGen.generateMonitoringInfo(adp);

        return monitoringInfo;
    }

    public static Map<String, Object> addMonitorInfo(MonitoringInfo monitoringInfo){

        String generatedApplicationId = UUID.randomUUID().toString();

        monitoringInfoByApplication.put(generatedApplicationId, monitoringInfo);

        HashMap<String, Object> appGroup = new HashMap<>();
        appGroup.put(MEMBERS, Arrays.asList(APPLICATION));
        Map<String, Object> policy = new HashMap<>();

        HashMap<String, String> policyProperties = new HashMap<>();
        policyProperties.put(ID, generatedApplicationId);
        policyProperties.put(TYPE, SEACLOUDS_MONITORING_RULES_ID_POLICY);
        policy.put(MONITORING_RULES_POLICY_NAME, policyProperties);

        ArrayList<Map<String, Object>> policiesList = new ArrayList<>();
        policiesList.add(policy);

        appGroup.put(POLICIES, policiesList);

        Yaml yml = new Yaml();
        Map<String, Object> adpYaml = (Map<String, Object>) yml.load(monitoringInfo.getReturnedAdp());
        Map<String, Object> groups = (Map<String, Object>) adpYaml.get(GROUPS);
        groups.put(MONITOR_INFO_GROUPNAME, appGroup);

        return adpYaml;
    }

    public static Map<String, Object> addSeaCloudsPolicy(Map<String, Object> adp,
                                                         MonitoringInfo monitoringInfo,
                                                         ApplicationMonitorId applicationMonitorId,
                                                         String slaEndpoint,
                                                         String t4cEndpoint,
                                                         String influxdbEndpoint,
                                                         String influxdbDatabase,
                                                         String influxdbUsername,
                                                         String influxdbPassword,
                                                         String grafanaEndpoint,
                                                         String grafanaUsername,
                                                         String grafanaPassword){

        String encodedAgreement = encodeAgreement(applicationMonitorId, slaEndpoint);

        Map<String, Object> seaCloudsPolicyConfiguration = MutableMap.of();
        seaCloudsPolicyConfiguration.put(TYPE, SEACLOUDS_INITIALIZER_POLICY);
        seaCloudsPolicyConfiguration.put("slaEndpoint",slaEndpoint);
        seaCloudsPolicyConfiguration.put("slaAgreement",encodedAgreement);
        seaCloudsPolicyConfiguration.put("t4cEndpoint",t4cEndpoint);
        seaCloudsPolicyConfiguration.put("t4cRules",encodeBase64MonitoringRules(monitoringInfo));
        seaCloudsPolicyConfiguration.put("influxdbEndpoint",influxdbEndpoint );
        seaCloudsPolicyConfiguration.put("influxdbDatabase",influxdbDatabase );
        seaCloudsPolicyConfiguration.put("influxdbUsername",influxdbUsername );
        seaCloudsPolicyConfiguration.put("influxdbPassword",influxdbPassword );
        seaCloudsPolicyConfiguration.put("grafanaEndpoint",grafanaEndpoint);
        seaCloudsPolicyConfiguration.put("grafanaUsername",grafanaUsername);
        seaCloudsPolicyConfiguration.put("grafanaPassword",grafanaPassword);

        Map<String, Object> seaCloudsPolicy = MutableMap.of();
        seaCloudsPolicy.put(SEACLOUDS_APPLICATION_CONFIGURATION_POLICY, seaCloudsPolicyConfiguration);

        Map<String, Object> seaCloudsApplicationGroup = MutableMap.of();
        seaCloudsApplicationGroup.put(MEMBERS, ImmutableList.of());
        seaCloudsApplicationGroup.put(POLICIES, ImmutableList.of(seaCloudsPolicy));

        Map<String, Object> groups = (Map<String, Object>) adp.get(GROUPS);
        groups.put(SEACLOUDS_APPLICATION_CONFIGURATION, seaCloudsApplicationGroup);

        return adp;
    }

    public static String encodeAgreement(ApplicationMonitorId applicationMonitorId, String slaEndpoint){
        List<NameValuePair> paremeters = MutableList.of((NameValuePair)
                new BasicNameValuePair("templateId", applicationMonitorId.getId()));
        String agreement = new HttpHelper(slaEndpoint).getRequest(
                "/seaclouds/commands/fromtemplate",
                paremeters);
        return Base64.encodeBase64String(agreement.getBytes());
    }

    public static String encodeBase64MonitoringRules(MonitoringInfo monitoringInfo){
        StringWriter sw = new StringWriter();
        String encodeMonitoringRules = null;
        JAXBContext jaxbContext = null;
        String marshalledMonitoringRules = null;
        try {
            jaxbContext = JAXBContext.newInstance(MonitoringRules.class);
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

            jaxbMarshaller.marshal(monitoringInfo.getApplicationMonitoringRules(), sw);
            marshalledMonitoringRules = sw.toString();

        } catch (JAXBException e) {
            log.error("Monitoring rules {} can not be marshalled by addSeaCloudsPolicy in " +
                            "DamGenerator",
                    monitoringInfo.getApplicationMonitoringRules());
        }

        encodeMonitoringRules= Base64
                .encodeBase64String(marshalledMonitoringRules.getBytes());
        return encodeMonitoringRules;
    }

    public static Map<String, Object> translateAPD(Map<String, Object> adpYaml){
        Yaml yml = new Yaml();
        DeployerTypesResolver deployerTypesResolver = getDeployerIaaSTypeResolver();

        Map<String, Object> damUsedNodeTypes = new HashMap<>();
        List<Object> groupsToAdd = new ArrayList<>();
        Map<String, ArrayList<String>> groups = new HashMap<>();
        Map<String, Object> ADPgroups = (Map<String, Object>) adpYaml.get(GROUPS);
        Map<String, Object> topologyTemplate = (Map<String, Object>) adpYaml.get(TOPOLOGY_TEMPLATE);
        Map<String, Object> nodeTemplates = (Map<String, Object>) topologyTemplate.get(NODE_TEMPLATES);
        Map<String, Object> nodeTypes = (Map<String, Object>) adpYaml.get(NODE_TYPES);

        for(String moduleName:nodeTemplates.keySet()){
            Map<String, Object> module = (Map<String, Object>) nodeTemplates.get(moduleName);

            ArrayList<Map<String, Object>> artifactsList =  (ArrayList<Map<String, Object>>) module.get("artifacts");
            if (artifactsList != null) {
                Map<String, Object> artifacts = artifactsList.get(0);
                artifacts.remove("type");

                Set<String> artifactKeys = artifacts.keySet();
                if (artifactKeys.size() > 1) {
                    throw new IllegalArgumentException();
                }

                String[] keys = artifactKeys.toArray(new String[1]);

                Map<String, Object> properties = (Map<String, Object>) module.get("properties");
                properties.put(keys[0], artifacts.get(keys[0]));

                module.remove("artifacts");
            }
            //type replacement
            String moduleType = (String) module.get("type");
            if(nodeTypes.containsKey(moduleType)){
                Map<String, Object> type = (HashMap<String, Object>) nodeTypes.get(moduleType);
                String sourceType = (String) type.get("derived_from");
                String targetType = deployerTypesResolver.resolveNodeType(sourceType);

                if (targetType != null) {
                    module.put("type", targetType);
                    if(deployerTypesResolver.getNodeTypeDefinition(targetType)!=null){
                        damUsedNodeTypes.put(targetType,
                                deployerTypesResolver.getNodeTypeDefinition(targetType));
                    } else {
                        log.error("TargetType definition " + targetType + "was not found" +
                                "so it will not added to DAM");
                    }
                } else {
                    damUsedNodeTypes.put(moduleType, nodeTypes.get(moduleType));
                }
            }

            if(module.keySet().contains(REQUIREMENTS)){
                List<Map<String, Object> > requirements = (ArrayList<Map<String, Object> >) module.get(REQUIREMENTS);
                for(Map<String, Object> req : requirements){
                    if(req.keySet().contains(HOST)){
                        String host = (String) req.get(HOST);
                        if(!groups.keySet().contains(host)){
                            groups.put(host, new ArrayList<String>());
                        }
                        groups.get(host).add(moduleName);
                    }
                    req.remove(INSTANCES_POC);
                }
            }
        }

        adpYaml.put(NODE_TYPES, damUsedNodeTypes);

        //get brookly location from host
        for(String group: groups.keySet()){
            HashMap<String, Object> policyGroup = new HashMap<>();
            policyGroup.put(MEMBERS, Arrays.asList(group));

            HashMap<String, Object> cloudOffering = (HashMap<String, Object>) nodeTemplates.get(group);
            HashMap<String, Object> properties = (HashMap<String, Object>) cloudOffering.get(PROPERTIES);

            String location = (String) properties.get(LOCATION);
            String region = (String) properties.get(REGION);
            String hardwareId = (String) properties.get(HARDWARE_ID);

            ArrayList<HashMap<String, Object>> policy = new ArrayList<>();
            HashMap<String, Object> p = new HashMap<>();
            if(location != null) {
                p.put(BROOKLYN_LOCATION, location + (location.equals(CLOUD_FOUNDRY) ? "" : ":" + region));
            }else{
                p.put(BROOKLYN_LOCATION, group);
            }
            policy.add(p);

            policyGroup.put(POLICIES, policy);

            HashMap<String, Object> finalGroup = new HashMap<>();

            ADPgroups.put(ADD_BROOKLYN_LOCATION + group ,policyGroup);
        }

        String finalDam = yml.dump(adpYaml);
        return adpYaml;
    }

    public static DeployerTypesResolver getDeployerIaaSTypeResolver(){
        try{
            if(deployerTypesResolver==null){
                deployerTypesResolver = new DeployerTypesResolver(Resources
                        .getResource(BROOKLYN_TYPES_MAPPING).toURI().toString());}
        }
        catch(Exception e){
            throw new RuntimeException(e);
        }
        return deployerTypesResolver;
    }

    public static Map<String, Object> addApplicationInfo(Map<String, Object> damYml,
                                                         ApplicationMonitorId applicationMonitorId,
                                                         String groupName){
        Map<String, Object> groups = (Map<String, Object>) damYml.get(GROUPS);

        try {
            HashMap<String, Object> appGroup = new HashMap<>();
            appGroup.put(MEMBERS, Arrays.asList(APPLICATION));

            Map<String, Object> policy = new HashMap<>();
            HashMap<String, String> policyProperties = new HashMap<>();
            policyProperties.put(ID, applicationMonitorId.id);
            policyProperties.put(TYPE, SEACLOUDS_APPLICATION_INFORMATION_POLICY_TYPE);
            policy.put(SEACLOUDS_APPLICATION_POLICY_NAME, policyProperties);

            ArrayList<Map<String, Object>> policiesList = new ArrayList<>();
            policiesList.add(policy);

            appGroup.put(POLICIES, policiesList);
            groups.put(groupName, appGroup);

        }catch(Exception e){
            log.error("Error adding " + groupName + " info", e);
        }
        return damYml;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> addPoliciesTypeIfNotPresent(Map<String, Object> groups){

        for(Map.Entry<String, Object> entryGroup: groups.entrySet()){
            List<Map<String, Object>> policies =
                    (List<Map<String, Object>>)((Map<String, Object>)entryGroup.getValue()).get(POLICIES);
            if(policies!=null){
                for(Map<String, Object> policy:policies){
                    String policyName = getPolicyName(policy);
                    if(!isLocationPolicy(policy)
                            && !(policy.get(policyName) instanceof String)){
                        Map<String, Object> policyProperties = getPolicyProperties(policy);

                        if(getPolicyType(policyProperties)==null){
                            policyProperties.put(TYPE, ((Object)"seaclouds.policies."+policyName));
                        } else {
                            translatePolicyToDeployerPolicy(policyProperties);
                        }
                    }
                }
            }
        }
        return null;
    }

    private static boolean isLocationPolicy(Map<String, Object> policy){

        return policy.containsKey(BROOKLYN_POLICY_TYPE);
    }

    private static String getPolicyType(Map<String, Object> policyProperties){
        return (String) policyProperties.get(TYPE);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> getPolicyProperties(Map<String, Object> policy){
        if(policy!=null) {
            for (Map.Entry<String, Object> policyEntry : policy.entrySet()) {
                return (Map<String, Object>) policyEntry.getValue();
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static String getPolicyName(Map<String, Object> policy){
        if(policy!=null) {
            for (Map.Entry<String, Object> policyEntry : policy.entrySet()) {
                return policyEntry.getKey();
            }
        }
        return null;
    }

    private static Map<String, Object> translatePolicyToDeployerPolicy(Map<String, Object> policyProperties){
        String deployerPolicyType = getDeployerIaaSTypeResolver()
                .resolvePolicyType(getPolicyType(policyProperties));
        if(deployerPolicyType!=null){
            policyProperties = resolverDeployerTypesInProperties(policyProperties);
            policyProperties.remove(TYPE);
            policyProperties.put(TYPE, deployerPolicyType);
        }
        return policyProperties;
    }

    private static Map<String, Object> resolverDeployerTypesInProperties(Map<String, Object> properties){
        String property, propertyName;
        for(Map.Entry<String, Object>entry: ImmutableMap.copyOf(properties).entrySet()){
            if(entry.getValue() instanceof String){
                property = (String) entry.getValue();
                propertyName = (String) entry.getKey();
                if(property.contains(SEACLOUDS_NODE_PREFIX)){
                    properties.remove(propertyName);
                    properties.put(propertyName, resolverDeployerTypesInAProperty(property));
                }
            }
        }
        return properties;
    }

    private static String resolverDeployerTypesInAProperty(String property){
        String[] slices = property.split("\"|\\s+|-|\\(|\\)|,");
        for(String slice: slices){
            if(getDeployerIaaSTypeResolver().resolveNodeType(slice)!=null){
                property = property.replaceAll(slice, getDeployerIaaSTypeResolver().resolveNodeType(slice));
            }
        }
        return property;
    }

}
