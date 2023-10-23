package com.contentgrid.surveyor.api.metrics;

import java.time.Instant;
import lombok.Builder;
import lombok.NonNull;
import org.reactivestreams.Publisher;

public interface ExportMetrics {

    Publisher<ExportedMetrics> findMetricsForExport(ExportMetricsCommand command);

    @Builder
    record ExportMetricsCommand(
            @NonNull
            String resourceType,
            @NonNull
            String metric,
            @NonNull
            Instant start,
            @NonNull
            Instant end
    ) {

    }

}
