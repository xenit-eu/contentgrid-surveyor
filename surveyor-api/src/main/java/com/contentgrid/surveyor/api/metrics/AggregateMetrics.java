package com.contentgrid.surveyor.api.metrics;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.NonNull;

public interface AggregateMetrics {

    Map<Resource, Metric> aggregateMetrics(AggregateMetricsCommand command);

    @Builder
    record AggregateMetricsCommand(
            @NonNull
            String system,
            @NonNull
            String resourceType,
            @NonNull
            String resourceId,
            Instant start,
            Instant end
    ) {

    }
}
