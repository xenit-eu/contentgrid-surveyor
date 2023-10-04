package com.contentgrid.surveyor.spi.storage;

import com.contentgrid.surveyor.spi.TimeInterval;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import lombok.Builder;

public interface AggregateEventCountMetricSpiPort {

    default EventCountMetric getAggregatedEventCountMetric(Resource resource, TimeInterval interval,
            GroupingConfiguration groupingConfiguration) {
        return Util.onlyValue(
                getAggregatedEventCountMetrics(resource, interval, interval.getDuration(), groupingConfiguration),
                () -> new EventCountMetric(interval, resource, BigDecimal.ZERO)
        );
    }

    List<EventCountMetric> getAggregatedEventCountMetrics(Resource resource, TimeInterval interval,
            Duration chunkDuration, GroupingConfiguration groupingConfiguration);

    @Builder
    record GroupingConfiguration(
            Duration groupInterval,
            GroupOperation operation
    ) {

        public enum GroupOperation {
            AVERAGE,
            MAX,
            MIN
        }
    }
}
