package com.contentgrid.surveyor.spi.config;

import com.contentgrid.surveyor.values.MetricName;
import com.contentgrid.surveyor.spi.MetricSourceSystemType;
import com.contentgrid.surveyor.values.ResourceType;
import java.time.Duration;
import lombok.Builder;

@Builder
public record MeasurementCollectionConfig(
        MetricSourceSystemType type,
        ResourceType resourceType,
        MetricName metric,
        String resourceIdLabel,
        String query,
        Duration interval
) {

}
