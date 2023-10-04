package com.contentgrid.surveyor.spi.source;

import java.time.Duration;
import lombok.Builder;

@Builder
public record MetricCollectionConfig(
        String type,
        String resourceType,
        String metric,
        String resourceIdLabel,
        String query,
        Duration interval
) {

}
