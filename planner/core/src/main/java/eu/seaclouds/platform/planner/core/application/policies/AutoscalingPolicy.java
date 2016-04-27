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
package eu.seaclouds.platform.planner.core.application.policies;

import java.util.Map;

import org.apache.brooklyn.util.collections.MutableMap;

import eu.seaclouds.platform.planner.core.DamGenerator;

public class AutoscalingPolicy {

    private static final String POLICY_TYPE = "org.apache.brooklyn.policy.autoscaling.AutoScalerPolicy";

    public static final String METRIC = "metric";
    private static final String RESIZABLE_DOWN_STABILIZATIONDELAY = "autoscaler.resizeDownStabilizationDelay";
    private static final String LOWER_BOUND = "metricLowerBound";
    private static final String UPPER_BOUND = "metricUpperBound";

    private static final String MIN_POOL_SIZE = "minPoolSize";
    private static final String MAX_POOL_SIZE = "maxPoolSize";

    private String metric;
    private Integer resizeDownStabilizationDelay;
    private Double metricLowerBound;
    private Double metricUpperBound;
    private Integer minPoolSize;
    private Integer maxPoolSize;

    private AutoscalingPolicy(Builder builder) {
        metric = builder.metric;
        resizeDownStabilizationDelay = builder.resizeDownStabilizationDelay;
        metricLowerBound = builder.metricLowerBound;
        metricUpperBound = builder.metricUpperBound;
        minPoolSize = builder.minPoolSize;
        maxPoolSize = builder.maxPoolSize;
    }

    public Map<String, String> getAutoscalingPolicyDefinition() {

        Map<String, String> policyDefinition = MutableMap.of();
        policyDefinition.put(DamGenerator.TYPE, POLICY_TYPE);
        policyDefinition.put(RESIZABLE_DOWN_STABILIZATIONDELAY, resizeDownStabilizationDelay.toString());
        policyDefinition.put(LOWER_BOUND, metricLowerBound.toString());
        policyDefinition.put(UPPER_BOUND, metricUpperBound.toString());
        policyDefinition.put(MIN_POOL_SIZE, minPoolSize.toString());
        policyDefinition.put(MAX_POOL_SIZE, maxPoolSize.toString());
        policyDefinition.put(METRIC, metric);

        return policyDefinition;
    }


    public static class Builder {

        private String metric;
        private Integer resizeDownStabilizationDelay;
        private Double metricLowerBound;
        private Double metricUpperBound;
        private Integer minPoolSize;
        private Integer maxPoolSize;

        public Builder() {

        }

        public Builder metric(String metric) {
            this.metric = metric;
            return this;
        }

        public Builder resizeDownStabilizationDelay(Integer resizeDownStabilizationDelay) {
            this.resizeDownStabilizationDelay = resizeDownStabilizationDelay;
            return this;
        }

        public Builder metricLowerBound(Double metricLowerBound) {
            this.metricLowerBound = metricLowerBound;
            return this;
        }

        public Builder metricUpperBound(Double metricUpperBound) {
            this.metricUpperBound = metricUpperBound;
            return this;
        }

        public Builder minPoolSize(Integer minPoolSize) {
            this.minPoolSize = minPoolSize;
            return this;
        }

        public Builder maxPoolSize(Integer maxPoolSize) {
            this.maxPoolSize = maxPoolSize;
            return this;
        }


        public Builder descriptionMap(Map<String, Object> valueMap) {

            if (valueMap.containsKey(RESIZABLE_DOWN_STABILIZATIONDELAY)) {
                this.resizeDownStabilizationDelay((Integer) valueMap.get(RESIZABLE_DOWN_STABILIZATIONDELAY));
            }
            if (valueMap.containsKey(LOWER_BOUND)) {
                this.metricLowerBound((Double) valueMap.get(LOWER_BOUND));
            }
            if (valueMap.containsKey(UPPER_BOUND)) {
                this.metricUpperBound((Double) valueMap.get(UPPER_BOUND));
            }
            if (valueMap.containsKey(MIN_POOL_SIZE)) {
                this.minPoolSize((Integer) valueMap.get(MIN_POOL_SIZE));
            }
            if (valueMap.containsKey(MAX_POOL_SIZE)) {
                this.maxPoolSize((Integer) valueMap.get(MAX_POOL_SIZE));
            }
            return this;
        }

        public AutoscalingPolicy build() {
            return new AutoscalingPolicy(this);
        }

    }

}
