package com.contentgrid.surveyor.api.metrics;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.NonNull;

public interface FindMetrics {

    Map<Resource, List<Metric>> findMetrics(FindMetricsCommand command);

    @Builder
    record FindMetricsCommand(
            @NonNull
            String system,
            @NonNull
            String resourceType,
            @NonNull
            String resourceId,
            Instant start,
            Instant end,
            Duration step
    ) {

    }

    record Metric(
            Instant startTime,
            Instant endTime,
            String metric,
            BigDecimal value
    ) {}

    record Resource(
            String metric
    ) {}
}
