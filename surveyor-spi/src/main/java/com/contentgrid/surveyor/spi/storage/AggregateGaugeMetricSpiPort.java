package com.contentgrid.surveyor.spi.storage;

import com.contentgrid.surveyor.spi.TimeInterval;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

public interface AggregateGaugeMetricSpiPort {

    default AggregatedGaugeMetric getAggregatedGaugeMetric(Resource resource, TimeInterval interval,
            AggregationConfiguration aggregation) {
        return Util.onlyValue(
                getAggregatedGaugeMetrics(resource, interval, aggregation,
                        Duration.between(interval.getStartTime(), interval.getEndTime())),
                () -> new AggregatedGaugeMetric(interval, resource, BigDecimal.ZERO)
        );
    }

    List<AggregatedGaugeMetric> getAggregatedGaugeMetrics(Resource resource, TimeInterval interval,
            AggregationConfiguration aggregation, Duration chunkDuration);

    @Value
    @Builder
    class AggregationConfiguration {

        @NonNull
        Duration averagingWindow;
    }
}
