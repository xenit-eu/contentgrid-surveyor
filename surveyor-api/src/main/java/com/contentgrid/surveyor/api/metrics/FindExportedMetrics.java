package com.contentgrid.surveyor.api.metrics;

import com.contentgrid.surveyor.values.MetricName;
import java.time.Instant;
import lombok.Builder;
import lombok.NonNull;
import org.reactivestreams.Publisher;

public interface FindExportedMetrics {

    Publisher<ExportedMetrics> findMetricsForExport(ExportMetricsCommand command);

    @Builder
    record ExportMetricsCommand(
            @NonNull
            MetricName metric,
            @NonNull
            Instant start,
            @NonNull
            Instant end
    ) {

    }

}
