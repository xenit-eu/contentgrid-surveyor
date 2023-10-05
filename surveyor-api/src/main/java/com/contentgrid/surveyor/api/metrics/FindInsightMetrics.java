package com.contentgrid.surveyor.api.metrics;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.NonNull;

public interface FindInsightMetrics {

    Map<Resource, List<Metric>> findMetricsForInsights(FindInsightMetricsCommand command);

    @Builder
    record FindInsightMetricsCommand(
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

}
