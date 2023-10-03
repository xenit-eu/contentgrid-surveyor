package com.contentgrid.surveyor.infrastructure.source.prometheus;


import java.time.Duration;
import lombok.Builder;

@Builder
public record PrometheusMetricCollectionConfig(
        PrometheusApiConfig api,
        String metric,
        String resourceType,
        String resourceIdLabel,
        String query,
        Duration snapshotInterval
) {

}
