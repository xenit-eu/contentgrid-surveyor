package com.contentgrid.surveyor.spi.source;

import java.time.Duration;
import lombok.Builder;

@Builder
public record MetricCollectionConfig(
        ResourceDefinition resourceDefinition,
        String resourceIdLabel,
        String query,
        Duration interval
) {

}
