package com.contentgrid.surveyor.api.metrics;

import java.time.Instant;
import java.util.Map;
import lombok.Builder;
import lombok.NonNull;
import org.reactivestreams.Publisher;

public interface BillingMetrics {

    Publisher<ResourceMetric> findMetricsForBilling(BillingMetricsCommand command);

    @Builder
    record BillingMetricsCommand(
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
