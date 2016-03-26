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
package eu.seaclouds.platform.planner.core;

import com.google.common.collect.Iterators;
import com.google.common.io.Resources;
import eu.seaclouds.platform.planner.core.facade.policies.SeaCloudsManagementPolicyFacade;
import org.apache.brooklyn.util.text.Strings;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class DamGeneratorTest {

    private static final String FAKE_AGREEMENT_ID = "agreement-1234567890";

    private static final String MONITOR_URL = "52.48.187.2";
    private static final String MONITOR_PORT = "8170";
    private static final String INFLUXDB_URL = "52.48.187.2";
    private static final String INFLUXDB_PORT = "8086";
    private static final String SLA_ENDPOINT = "127.0.0.3:9003";
    private static final String INFLUXDB_DATABASE = "tower4clouds";
    private static final String INFLUXDB_USERNAME = "root";
    private static final String INFLUXDB_PASSWORD = "root";
    private static final String GRAFANA_USERNAME = "admin";
    private static final String GRAFANA_PASSWORD = "admin";
    private static final String GRAFANA_ENDPOINT = "http://127.0.0.4:1234";

    Yaml yamlParser;
    String dam = null;
    Map<String, Object> template = null;

    @Mock
    private DamGenerator.SlaAgreementManager fakeAgreementManager;

    @BeforeMethod
    public void setUp() throws URISyntaxException, FileNotFoundException {
        MockitoAnnotations.initMocks(this);

        when(fakeAgreementManager.generateAgreeemntId(((Map<String, Object>) anyObject())))
                .thenReturn(FAKE_AGREEMENT_ID);
        String fakeAgreement = new Scanner(new File(Resources.getResource("agreements/mock_test_agreement.xml").toURI())).useDelimiter("\\Z").next();
        when(fakeAgreementManager.getAgreement(anyString())).thenReturn(fakeAgreement);

        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        yamlParser = new Yaml(options);
    }

    private DamGenerator getDamGenerator() {
        DamGenerator damGenerator = new DamGenerator.Builder()
                .monitorUrl(MONITOR_URL)
                .monitorPort(MONITOR_PORT)
                .slaUrl(SLA_ENDPOINT)
                .influxdbUrl(INFLUXDB_URL)
                .influxdbPort(INFLUXDB_PORT)
                .influxdbDatabase(INFLUXDB_DATABASE)
                .influxdbUsername(INFLUXDB_USERNAME)
                .influxdbPassword(INFLUXDB_PASSWORD)
                .grafanaUsername(GRAFANA_USERNAME)
                .grafanaPassword(GRAFANA_PASSWORD)
                .grafanaEndpoint(GRAFANA_ENDPOINT)
                .build();
        return damGenerator;
    }

    private static String getMonitorEndpoint() {
        return "http://" + MONITOR_URL + ":" + MONITOR_PORT;
    }

    private static String getInfluxDbEndpoint() {
        return "http://" + INFLUXDB_URL + ":" + INFLUXDB_PORT;
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testMetadataTemplate() throws Exception {
        String adp = new Scanner(new File(Resources.getResource("nuro/iaas/nuro_adp-iaas.yml").toURI())).useDelimiter("\\Z").next();

        DamGenerator damGenerator = getDamGenerator();
        damGenerator.setAgreementManager(fakeAgreementManager);
        dam = damGenerator.generateDam(adp);
        template = (Map<String, Object>) yamlParser.load(dam);

        assertNotNull(template);
        assertNotNull(template.get(DamGenerator.TEMPLATE_NAME));
        assertTrue(((String) template.get(DamGenerator.TEMPLATE_NAME)).contains(DamGenerator.TEMPLATE_NAME_PREFIX));

        assertNotNull(template.get(DamGenerator.TEMPLATE_VERSION));
        assertTrue(((String) template.get(DamGenerator.TEMPLATE_VERSION)).contains(DamGenerator.DEFAULT_TEMPLATE_VERSION));

        assertNotNull(template.get(DamGenerator.IMPORTS));
        assertTrue(template.get(DamGenerator.IMPORTS) instanceof List);
        List imports = (List) template.get(DamGenerator.IMPORTS);
        assertEquals(imports.size(), 2);
        assertTrue(imports.contains(DamGenerator.TOSCA_NORMATIVE_TYPES + ":" + DamGenerator.TOSCA_NORMATIVE_TYPES_VERSION));
        assertTrue(imports.contains(DamGenerator.SEACLOUDS_NODE_TYPES + ":" + DamGenerator.SEACLOUDS_NODE_TYPES_VERSION));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGroupsAsTopologyChild() throws Exception {
        String adp = new Scanner(new File(Resources.getResource("nuro/iaas/nuro_adp-iaas.yml").toURI())).useDelimiter("\\Z").next();

        DamGenerator damGenerator = getDamGenerator();
        damGenerator.setAgreementManager(fakeAgreementManager);
        dam = damGenerator.generateDam(adp);
        template = (Map<String, Object>) yamlParser.load(dam);

        assertTrue(template.containsKey(DamGenerator.TOPOLOGY_TEMPLATE));
        Map<String, Object> topologyTemplate =
                (Map<String, Object>) template.get(DamGenerator.TOPOLOGY_TEMPLATE);

        assertTrue(topologyTemplate.containsKey(DamGenerator.GROUPS));
        Map<String, Object> topologyGroups =
                (Map<String, Object>) topologyTemplate.get(DamGenerator.GROUPS);

        assertNotNull(topologyGroups);
        assertEquals(topologyGroups.size(), 7);
        assertTrue(topologyGroups.containsKey("operation_www"));
        assertTrue(topologyGroups.containsKey("operation_db"));
        assertTrue(topologyGroups.containsKey("add_brooklyn_location_Amazon_EC2_m1_small_eu_central_1"));
        assertTrue(topologyGroups.containsKey("add_brooklyn_location_Amazon_EC2_m4_10xlarge_eu_west_1"));
        assertTrue(topologyGroups.containsKey("monitoringInformation"));
        assertTrue(topologyGroups.containsKey("sla_gen_info"));
        testSeaCloudsPolicy(topologyGroups);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testNuroDamGenerationForIaaS() throws Exception {
        String adp = new Scanner(new File(Resources.getResource("nuro/iaas/nuro_adp-iaas.yml").toURI())).useDelimiter("\\Z").next();

        DamGenerator damGenerator = getDamGenerator();
        damGenerator.setAgreementManager(fakeAgreementManager);
        dam = damGenerator.generateDam(adp);
        template = (Map<String, Object>) yamlParser.load(dam);

        String expectedDamString = new Scanner(new File(Resources.getResource("nuro/iaas/nuro_dam-iaas.yml").toURI())).useDelimiter("\\Z").next();
        Map<String, Object> expectedDam = (Map<String, Object>) yamlParser.load(expectedDamString);

        assertNotNull(template);

        Map<String, Object> generatedTopologyTemplate = (Map<String, Object>) template.get(DamGenerator.TOPOLOGY_TEMPLATE);
        Map<String, Object> expectedTopologyTemplate = (Map<String, Object>) expectedDam.get(DamGenerator.TOPOLOGY_TEMPLATE);

        Map<String, Object> generatedNodeTemplates = (Map<String, Object>) generatedTopologyTemplate.get(DamGenerator.NODE_TEMPLATES);
        Map<String, Object> expectedNodeTemplates = (Map<String, Object>) expectedTopologyTemplate.get(DamGenerator.NODE_TEMPLATES);

        /* checkapplication modules */
        assertEquals(generatedNodeTemplates.get("www"), expectedNodeTemplates.get("www"));
        assertEquals(generatedNodeTemplates.get("db"), expectedNodeTemplates.get("db"));

        /* check data collector */
        assertEquals(generatedNodeTemplates.get("modacloudsDc_www"), expectedNodeTemplates.get("modacloudsDc_www"));
        assertEquals(generatedNodeTemplates.get("seacloudsDc_www"), expectedNodeTemplates.get("seacloudsDc_www"));
        assertEquals(generatedNodeTemplates.get("seacloudsDc_db"), expectedNodeTemplates.get("seacloudsDc_db"));

        /* check offerings */
        assertEquals(generatedNodeTemplates.get("Amazon_EC2_m1_small_eu_central_1"), expectedNodeTemplates.get("Amazon_EC2_m1_small_eu_central_1"));
        assertEquals(generatedNodeTemplates.get("Amazon_EC2_m4_10xlarge_eu_west_1"), expectedNodeTemplates.get("Amazon_EC2_m4_10xlarge_eu_west_1"));

        Map<String, Object> generatedGroups = (Map<String, Object>) generatedTopologyTemplate.get(DamGenerator.GROUPS);
        Map<String, Object> expectedGroups = (Map<String, Object>) expectedTopologyTemplate.get(DamGenerator.GROUPS);
        testSeaCloudsPolicy(generatedGroups);

        assertEquals(generatedGroups.get("operation_www"), expectedGroups.get("operation_www"));
        assertEquals(generatedGroups.get("operation_db"), expectedGroups.get("operation_db"));
        assertEquals(generatedGroups.get("add_brooklyn_location_Amazon_EC2_m1_small_eu_central_1"), expectedGroups.get("add_brooklyn_location_Amazon_EC2_m1_small_eu_central_1"));
        assertEquals(generatedGroups.get("add_brooklyn_location_Amazon_EC2_m4_10xlarge_eu_west_1"), expectedGroups.get("add_brooklyn_location_Amazon_EC2_m4_10xlarge_eu_west_1"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testNuroDamGenerationForPaaS() throws Exception {
        String adp = new Scanner(new File(Resources.getResource("nuro/paas/nuro_adp-paas.yml").toURI())).useDelimiter("\\Z").next();

        DamGenerator damGenerator = getDamGenerator();
        damGenerator.setAgreementManager(fakeAgreementManager);
        dam = damGenerator.generateDam(adp);
        template = (Map<String, Object>) yamlParser.load(dam);

        String expectedDamString = new Scanner(new File(Resources.getResource("nuro/paas/nuro_dam-paas.yml").toURI())).useDelimiter("\\Z").next();
        Map<String, Object> expectedDam = (Map<String, Object>) yamlParser.load(expectedDamString);

        assertNotNull(template);

        Map<String, Object> generatedTopologyTemplate = (Map<String, Object>) template.get(DamGenerator.TOPOLOGY_TEMPLATE);
        Map<String, Object> expectedTopologyTemplate = (Map<String, Object>) expectedDam.get(DamGenerator.TOPOLOGY_TEMPLATE);

        Map<String, Object> generatedNodeTemplates = (Map<String, Object>) generatedTopologyTemplate.get(DamGenerator.NODE_TEMPLATES);
        Map<String, Object> expectedNodeTemplates = (Map<String, Object>) expectedTopologyTemplate.get(DamGenerator.NODE_TEMPLATES);

        /* check application modules */
        assertEquals(generatedNodeTemplates.get("php"), expectedNodeTemplates.get("php"));
        assertEquals(generatedNodeTemplates.get("db"), expectedNodeTemplates.get("db"));

        /* check data collector */
        assertEquals(generatedNodeTemplates.get("modacloudsDc_db"), expectedNodeTemplates.get("modacloudsDc_db"));
        assertEquals(generatedNodeTemplates.get("seacloudsDc_db"), expectedNodeTemplates.get("seacloudsDc_db"));

        /* check offerings */
        assertEquals(generatedNodeTemplates.get("Amazon_EC2_c3_2xlarge_ap_southeast_2"),
                expectedNodeTemplates.get("Amazon_EC2_c3_2xlarge_ap_southeast_2"));

        Map<String, Object> generatedGroups = (Map<String, Object>) generatedTopologyTemplate.get(DamGenerator.GROUPS);
        Map<String, Object> expectedGroups = (Map<String, Object>) expectedTopologyTemplate.get(DamGenerator.GROUPS);
        testSeaCloudsPolicy(generatedGroups);

        assertEquals(generatedGroups.get("operation_php"), expectedGroups.get("operation_php"));
        assertEquals(generatedGroups.get("operation_db"), expectedGroups.get("operation_db"));

        assertEquals(generatedGroups.get("add_brooklyn_location_Amazon_EC2_c3_2xlarge_ap_southeast_2"),
                expectedGroups.get("add_brooklyn_location_Amazon_EC2_c3_2xlarge_ap_southeast_2"));
        assertEquals(generatedGroups.get("add_brooklyn_location_php"),
                expectedGroups.get("add_brooklyn_location_php"));

        Map<String, Object> monitoringInformation = (Map<String, Object>) generatedGroups.get("monitoringInformation");
        testMonitoringInformation(monitoringInformation);

        assertEquals(generatedGroups.get("sla_gen_info"), expectedGroups.get("sla_gen_info"));

        assertEquals(generatedGroups.get("seaclouds_configuration_policy"),
                expectedGroups.get("seaclouds_configuration_policy"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testAtosDam() throws Exception {
        String adp = new Scanner(new File(Resources.getResource("atos/atos_adp.yml").toURI())).useDelimiter("\\Z").next();

        DamGenerator damGenerator = getDamGenerator();
        damGenerator.setAgreementManager(fakeAgreementManager);
        dam = damGenerator.generateDam(adp);
        template = (Map<String, Object>) yamlParser.load(dam);

        String expectedDamString = new Scanner(new File(Resources.getResource("atos/atos_dam.yml").toURI())).useDelimiter("\\Z").next();
        Map<String, Object> expectedDam = (Map<String, Object>) yamlParser.load(expectedDamString);

        assertNotNull(template);

        Map<String, Object> generatedTopologyTemplate = (Map<String, Object>) template.get(DamGenerator.TOPOLOGY_TEMPLATE);
        Map<String, Object> expectedTopologyTemplate = (Map<String, Object>) expectedDam.get(DamGenerator.TOPOLOGY_TEMPLATE);

        Map<String, Object> generatedNodeTemplates = (Map<String, Object>) generatedTopologyTemplate.get(DamGenerator.NODE_TEMPLATES);
        Map<String, Object> expectedNodeTemplates = (Map<String, Object>) expectedTopologyTemplate.get(DamGenerator.NODE_TEMPLATES);

        /* checkapplication modules */
        assertEquals(generatedNodeTemplates.get("www"), expectedNodeTemplates.get("www"));
        assertEquals(generatedNodeTemplates.get("webservices"), expectedNodeTemplates.get("webservices"));
        assertEquals(generatedNodeTemplates.get("db1"), expectedNodeTemplates.get("db1"));

        /* check data collector */
        assertEquals(generatedNodeTemplates.get("modacloudsDc_www"), expectedNodeTemplates.get("modacloudsDc_www"));
        assertEquals(generatedNodeTemplates.get("javaAppDc_www"), expectedNodeTemplates.get("javaAppDc_www"));
        assertEquals(generatedNodeTemplates.get("seacloudsDc_www"), expectedNodeTemplates.get("seacloudsDc_www"));
        assertEquals(generatedNodeTemplates.get("modacloudsDc_webservices"), expectedNodeTemplates.get("modacloudsDc_webservices"));
        assertEquals(generatedNodeTemplates.get("javaAppDc_webservices"), expectedNodeTemplates.get("javaAppDc_webservices"));
        assertEquals(generatedNodeTemplates.get("seacloudsDc_webservices"), expectedNodeTemplates.get("seacloudsDc_webservices"));
        assertEquals(generatedNodeTemplates.get("modacloudsDc_db1"), expectedNodeTemplates.get("modacloudsDc_db1"));
        assertEquals(generatedNodeTemplates.get("seacloudsDc_db1"), expectedNodeTemplates.get("seacloudsDc_db1"));

        /* check offerings */
        assertEquals(generatedNodeTemplates.get("Amazon_EC2_c1_xlarge_eu_central_1"), expectedNodeTemplates.get("Amazon_EC2_c1_xlarge_eu_central_1"));
        assertEquals(generatedNodeTemplates.get("Amazon_EC2_m4_large_eu_west_1"), expectedNodeTemplates.get("Amazon_EC2_m4_large_eu_west_1"));
        assertEquals(generatedNodeTemplates.get("Amazon_EC2_t2_micro_us_east_1"), expectedNodeTemplates.get("Amazon_EC2_t2_micro_us_east_1"));

        Map<String, Object> generatedGroups = (Map<String, Object>) generatedTopologyTemplate.get(DamGenerator.GROUPS);
        Map<String, Object> expectedGroups = (Map<String, Object>) expectedTopologyTemplate.get(DamGenerator.GROUPS);
        testSeaCloudsPolicy(generatedGroups);

        assertEquals(generatedGroups.get("operation_www"), expectedGroups.get("operation_www"));
        assertEquals(generatedGroups.get("operation_webservices"), expectedGroups.get("operation_webservices"));
        assertEquals(generatedGroups.get("operation_db1"), expectedGroups.get("operation_db1"));
        assertEquals(generatedGroups.get("add_brooklyn_location_Amazon_EC2_c1_xlarge_eu_central_1"), expectedGroups.get("add_brooklyn_location_Amazon_EC2_c1_xlarge_eu_central_1"));
        assertEquals(generatedGroups.get("add_brooklyn_location_Amazon_EC2_t2_micro_us_east_1"), expectedGroups.get("add_brooklyn_location_Amazon_EC2_t2_micro_us_east_1"));
        assertEquals(generatedGroups.get("add_brooklyn_location_Amazon_EC2_m4_large_eu_west_1"), expectedGroups.get("add_brooklyn_location_Amazon_EC2_m4_large_eu_west_1"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testWebChatDam() throws Exception {
        String adp = new Scanner(new File(Resources.getResource("webchat/webchat_adp.yml").toURI())).useDelimiter("\\Z").next();

        DamGenerator damGenerator = getDamGenerator();
        damGenerator.setAgreementManager(fakeAgreementManager);
        dam = damGenerator.generateDam(adp);
        template = (Map<String, Object>) yamlParser.load(dam);

        String expectedDamString = new Scanner(new File(Resources.getResource("webchat/webchat_dam.yml").toURI())).useDelimiter("\\Z").next();
        Map<String, Object> expectedDam = (Map<String, Object>) yamlParser.load(expectedDamString);

        assertNotNull(template);

        Map<String, Object> generatedTopologyTemplate = (Map<String, Object>) template.get(DamGenerator.TOPOLOGY_TEMPLATE);
        Map<String, Object> expectedTopologyTemplate = (Map<String, Object>) expectedDam.get(DamGenerator.TOPOLOGY_TEMPLATE);

        Map<String, Object> generatedNodeTemplates = (Map<String, Object>) generatedTopologyTemplate.get(DamGenerator.NODE_TEMPLATES);
        Map<String, Object> expectedNodeTemplates = (Map<String, Object>) expectedTopologyTemplate.get(DamGenerator.NODE_TEMPLATES);

        /* checkapplication modules */
        assertEquals(generatedNodeTemplates.get("Chat"), expectedNodeTemplates.get("Chat"));
        assertEquals(generatedNodeTemplates.get("MessageDatabase"), expectedNodeTemplates.get("MessageDatabase"));

        /* check data collector */
        assertEquals(generatedNodeTemplates.get("modacloudsDc_Chat"), expectedNodeTemplates.get("modacloudsDc_Chat"));
        assertEquals(generatedNodeTemplates.get("javaAppDc_Chat"), expectedNodeTemplates.get("javaAppDc_Chat"));
        assertEquals(generatedNodeTemplates.get("seacloudsDc_Chat"), expectedNodeTemplates.get("seacloudsDc_Chat"));
        assertEquals(generatedNodeTemplates.get("modacloudsDc_MessageDatabase"), expectedNodeTemplates.get("modacloudsDc_MessageDatabase"));
        assertEquals(generatedNodeTemplates.get("seacloudsDc_MessageDatabase"), expectedNodeTemplates.get("seacloudsDc_MessageDatabase"));

        /* check offerings */
        assertEquals(generatedNodeTemplates.get("Amazon_EC2_c1_medium_sa_east_1"), expectedNodeTemplates.get("Amazon_EC2_c1_medium_sa_east_1"));
        assertEquals(generatedNodeTemplates.get("Amazon_EC2_c1_medium_us_west_2"), expectedNodeTemplates.get("Amazon_EC2_c1_medium_us_west_2"));

        Map<String, Object> generatedGroups = (Map<String, Object>) generatedTopologyTemplate.get(DamGenerator.GROUPS);
        Map<String, Object> expectedGroups = (Map<String, Object>) expectedTopologyTemplate.get(DamGenerator.GROUPS);
        testSeaCloudsPolicy(generatedGroups);

        assertEquals(generatedGroups.get("operation_Chat"), expectedGroups.get("operation_Chat"));
        assertEquals(generatedGroups.get("operation_MessageDatabase"), expectedGroups.get("operation_MessageDatabase"));
        assertEquals(generatedGroups.get("add_brooklyn_location_Amazon_EC2_c1_medium_us_west_2"), expectedGroups.get("add_brooklyn_location_Amazon_EC2_c1_medium_us_west_2"));
        assertEquals(generatedGroups.get("add_brooklyn_location_Amazon_EC2_c1_medium_sa_east_1"), expectedGroups.get("add_brooklyn_location_Amazon_EC2_c1_medium_sa_east_1"));
    }


    public void testSeaCloudsPolicy(Map<String, Object> groups) {
        assertNotNull(groups);
        assertTrue(groups.containsKey(DamGenerator.SEACLOUDS_APPLICATION_CONFIGURATION));
        Map<String, Object> policyGroup = (Map<String, Object>) groups
                .get(DamGenerator.SEACLOUDS_APPLICATION_CONFIGURATION);

        assertTrue(policyGroup.containsKey(DamGenerator.MEMBERS));
        assertTrue(policyGroup.get(DamGenerator.MEMBERS) instanceof List);
        assertTrue(((List) policyGroup.get(DamGenerator.MEMBERS)).isEmpty());

        assertTrue(policyGroup.containsKey(DamGenerator.POLICIES));
        assertTrue(policyGroup.get(DamGenerator.POLICIES) instanceof List);
        List<Object> policies = (List<Object>) policyGroup.get(DamGenerator.POLICIES);

        assertEquals(policies.size(), 1);
        assertTrue(policies.get(0) instanceof Map);
        Map<String, Object> seacloudsManagementPolicy = (Map<String, Object>) policies.get(0);
        assertEquals(seacloudsManagementPolicy.size(), 1);
        assertTrue(seacloudsManagementPolicy.containsKey(SeaCloudsManagementPolicyFacade.SEACLOUDS_APPLICATION_CONFIGURATION_POLICY));

        Map<String, Object> seacloudsManagementPolicyProperties = (Map<String, Object>)
                seacloudsManagementPolicy.get(SeaCloudsManagementPolicyFacade.SEACLOUDS_APPLICATION_CONFIGURATION_POLICY);
        assertEquals(seacloudsManagementPolicyProperties.size(), 12);

        assertEquals(seacloudsManagementPolicyProperties.get(DamGenerator.TYPE),
                SeaCloudsManagementPolicyFacade.SEACLOUDS_MANAGEMENT_POLICY);
        assertEquals(seacloudsManagementPolicyProperties.get(SeaCloudsManagementPolicyFacade.SLA_ENDPOINT), SLA_ENDPOINT);
        assertFalse(Strings.isBlank((String) seacloudsManagementPolicyProperties.get(SeaCloudsManagementPolicyFacade.SLA_AGREEMENT)));
        assertEquals(seacloudsManagementPolicyProperties.get(SeaCloudsManagementPolicyFacade.T4C_ENDPOINT), getMonitorEndpoint());
        assertFalse(Strings.isBlank((String) seacloudsManagementPolicyProperties.get(SeaCloudsManagementPolicyFacade.T4C_RULES)));
        assertEquals(seacloudsManagementPolicyProperties.get(SeaCloudsManagementPolicyFacade.INFLUXDB_ENDPOINT), getInfluxDbEndpoint());
        assertEquals(seacloudsManagementPolicyProperties.get(SeaCloudsManagementPolicyFacade.INFLUXDB_DATABASE), INFLUXDB_DATABASE);
        assertEquals(seacloudsManagementPolicyProperties.get(SeaCloudsManagementPolicyFacade.INFLUXDB_USERNAME), INFLUXDB_USERNAME);
        assertEquals(seacloudsManagementPolicyProperties.get(SeaCloudsManagementPolicyFacade.INFLUXDB_PASSWORD), INFLUXDB_PASSWORD);
        assertEquals(seacloudsManagementPolicyProperties.get(SeaCloudsManagementPolicyFacade.GRAFANA_ENDPOINT), GRAFANA_ENDPOINT);
        assertEquals(seacloudsManagementPolicyProperties.get(SeaCloudsManagementPolicyFacade.GRAFANA_USERNAME), GRAFANA_USERNAME);
        assertEquals(seacloudsManagementPolicyProperties.get(SeaCloudsManagementPolicyFacade.GRAFANA_PASSWORD), GRAFANA_PASSWORD);
    }

    public void testMonitoringInformation(Map<String, Object> monitoringInformation){
        assertTrue(monitoringInformation.containsKey(DamGenerator.POLICIES));
        List<Map<String, Object>> policies =
                (List<Map<String, Object>>) monitoringInformation.get(DamGenerator.POLICIES);
        assertEquals(policies.size(), 1);

        Map<String, Object> policy = Iterators.getOnlyElement(policies.iterator());
        Map<String, Object> policyValues = (Map<String, Object> )policy.get("monitoringrules.information.policy");
        assertTrue(policyValues.containsKey("id"));
        assertFalse(Strings.isBlank((String) policyValues.get("id")));

        assertTrue(policyValues.containsKey(DamGenerator.TYPE));
        assertTrue(Strings.isBlank((String) policyValues.get("seaclouds.policies.monitoringrules")));

        assertTrue(monitoringInformation.containsKey(DamGenerator.MEMBERS));
        List<String> members =
                (List<String>) monitoringInformation.get(DamGenerator.MEMBERS);
        assertEquals(members.size(), 1);
        assertEquals(Iterators.getOnlyElement(members.iterator()), "application");
    }
}