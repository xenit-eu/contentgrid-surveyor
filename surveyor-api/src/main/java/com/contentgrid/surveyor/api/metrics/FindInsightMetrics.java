package com.contentgrid.surveyor.api.metrics;

import com.contentgrid.surveyor.values.ResourceId;
import com.contentgrid.surveyor.values.ResourceType;
import com.contentgrid.surveyor.values.SourceName;
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
            SourceName system,
            @NonNull
            ResourceType resourceType,
            @NonNull
            ResourceId resourceId,
            Instant start,
            Instant end,
            Duration step
    ) {

    }

}
