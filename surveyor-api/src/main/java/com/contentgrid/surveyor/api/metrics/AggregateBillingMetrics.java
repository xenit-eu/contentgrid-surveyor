package com.contentgrid.surveyor.api.metrics;

import com.contentgrid.surveyor.spi.resources.LinkedMeasurements;
import java.time.Instant;
import lombok.Builder;
import reactor.core.publisher.Flux;

public interface AggregateBillingMetrics {

    Flux<LinkedMeasurements> findMetricsForBilling(AggregateBillingMetricsCommand command);

    record AggregateBillingMetricsCommand(Instant start, Instant end) {}
}
