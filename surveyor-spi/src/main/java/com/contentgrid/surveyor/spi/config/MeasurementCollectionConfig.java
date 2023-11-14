package com.contentgrid.surveyor.spi.config;

import com.contentgrid.surveyor.values.MetricName;
import com.contentgrid.surveyor.spi.MetricSourceSystemType;
import com.contentgrid.surveyor.values.ResourceType;
import java.time.Duration;
import lombok.Builder;
import lombok.NonNull;

@Builder
public record MeasurementCollectionConfig(
        @NonNull
        MetricSourceSystemType type,
        @NonNull
        ResourceType resourceType,
        @NonNull
        MetricName metric,
        @NonNull
        String resourceIdLabel,
        @NonNull
        String query,
        @NonNull
        Duration interval
) {

}
