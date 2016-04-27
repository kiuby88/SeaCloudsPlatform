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
package eu.seaclouds.platform.planner.core.application.decorators;

import java.util.List;
import java.util.Map;

import org.apache.brooklyn.util.text.Strings;

import com.google.common.base.Optional;
import com.google.common.collect.Iterators;

import eu.seaclouds.platform.planner.core.DamGenerator;
import eu.seaclouds.platform.planner.core.application.ApplicationFacade;
import eu.seaclouds.platform.planner.core.application.policies.AutoscalingPolicy;

public class AutoscalingPoliciesDecorator implements ApplicationFacadeDecorator {

    private static final String SEACLOUD_AUTOSCALING_POLICy = "seaclouds.policies.autoscaling.AutoScalerPolicy";

    private Map<String, Object> groups;
    private ApplicationFacade applicationFacade;

    @Override
    public void apply(ApplicationFacade applicationFacade) {
        this.applicationFacade = applicationFacade;
        this.groups = applicationFacade.getGroups();

        processAutoscalingPolicies();
    }

    @SuppressWarnings("unchecked")
    private void processAutoscalingPolicies() {
        for (String groupId : groups.keySet()) {

            Map<String, Object> groupDefinition = (Map<String, Object>) groups.get(groupId);
            List<String> members = (List<String>) groupDefinition.get(DamGenerator.MEMBERS);
            List<Map<String, Object>> policies = (List<Map<String, Object>>) groupDefinition.get(DamGenerator.POLICIES);
            convertAutoscalingPolicies(members, policies);
        }

    }

    private void convertAutoscalingPolicies(List<String> members, List<Map<String, Object>> policies) {
        for (Map<String, Object> policy : policies) {
            String policyId = getPolicyId(policy);
            Map<String, Object> policyDescription = (Map<String, Object>) policy.get(policyId);
            if (isAutoscalingPolicy(policyDescription)) {
                AutoscalingPolicy autoscalingPolicy =
                        createAutoscalingPolicy(policyDescription,
                                Iterators.getOnlyElement(members.iterator()));
                policy.put(policyId, autoscalingPolicy.getAutoscalingPolicyDefinition());
            }
        }
    }

    private boolean isAutoscalingPolicy(Map<String, Object> policyDescription) {
        String type = (String) policyDescription.get(DamGenerator.TYPE);
        return (!Strings.isBlank(type)) && (type.equalsIgnoreCase(SEACLOUD_AUTOSCALING_POLICy));
    }

    private String getPolicyId(Map<String, Object> policy) {
        return Iterators.getOnlyElement(policy.keySet().iterator());

    }

    private AutoscalingPolicy createAutoscalingPolicy(Map<String, Object> policyDescription, String memberId) {
        String seacloudsMetric = getMetric(policyDescription);
        Optional<String> resolvedMetric = applicationFacade.resolveMetric(memberId, seacloudsMetric);
        if (resolvedMetric.isPresent()) {
            return createAutoscalingPolicy(resolvedMetric.get(), policyDescription);
        } else {
            throw new RuntimeException("Metric not valid " + seacloudsMetric + " for node "
                    + memberId);
        }
    }

    private AutoscalingPolicy createAutoscalingPolicy(String metric, Map<String, Object> policyDescription) {
        return new AutoscalingPolicy.Builder()
                .descriptionMap(policyDescription)
                .metric(metric)
                .build();
    }

    private String getMetric(Map<String, Object> policyDescription) {
        return (String) policyDescription.get(AutoscalingPolicy.METRIC);
    }

}
