package com.contentgrid.surveyor.values;

public record ResourceAndMetric(ResourceType resource, MetricName metric) {
    public static ResourceAndMetric of(String resourceType, String metricName) {
        return new ResourceAndMetric(ResourceType.of(resourceType), MetricName.of(metricName));
    }
}
