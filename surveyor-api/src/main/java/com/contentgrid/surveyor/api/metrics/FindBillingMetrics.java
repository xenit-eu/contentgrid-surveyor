package com.contentgrid.surveyor.api.metrics;

import com.contentgrid.surveyor.values.ResourceId;
import com.contentgrid.surveyor.values.ResourceType;
import com.contentgrid.surveyor.values.SourceName;
import java.time.Instant;
import lombok.Builder;
import lombok.NonNull;
import org.reactivestreams.Publisher;

public interface FindBillingMetrics {

    Publisher<ResourceMetric> findMetricsForBilling(BillingMetricsCommand command);

    @Builder
    record BillingMetricsCommand(
            @NonNull
            SourceName system,
            @NonNull
            ResourceType resourceType,
            @NonNull
            ResourceId resourceId,
            Instant start,
            Instant end
    ) {

    }
}
