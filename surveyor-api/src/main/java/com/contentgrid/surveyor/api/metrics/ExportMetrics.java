package com.contentgrid.surveyor.api.metrics;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.NonNull;

public interface ExportMetrics {
    Map<Resource, List<Metric>> findMetricsForExport(ExportMetricsCommand command);

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
