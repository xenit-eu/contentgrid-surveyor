package com.contentgrid.surveyor.api.metrics;

import java.time.Duration;
import java.time.Instant;
import lombok.Builder;
import lombok.NonNull;
import org.reactivestreams.Publisher;

public interface FindInsightMetrics {

    Publisher<ExportedMetrics> findMetricsForInsights(FindInsightMetricsCommand command);

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
