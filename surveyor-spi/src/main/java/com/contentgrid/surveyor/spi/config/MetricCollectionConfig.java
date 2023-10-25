package com.contentgrid.surveyor.spi.config;

import com.contentgrid.surveyor.values.MetricName;
import com.contentgrid.surveyor.spi.MetricSourceSystemType;
import java.time.Duration;
import lombok.Builder;

@Builder
public record MetricCollectionConfig(
        MetricSourceSystemType type,
        MetricName metric,
        String resourceIdLabel,
        String query,
        Duration interval
) {

}
