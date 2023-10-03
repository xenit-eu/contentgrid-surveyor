package com.contentgrid.surveyor.spi.storage;

import com.contentgrid.surveyor.spi.TimeInterval;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

public interface AggregateEventCountMetricSpiPort {

    default EventCountMetric getAggregatedEventCountMetric(Resource resource, TimeInterval interval) {
        return Util.onlyValue(
                getAggregatedEventCountMetrics(resource, interval,
                        Duration.between(interval.getStartTime(), interval.getEndTime())),
                () -> new EventCountMetric(interval, resource, BigDecimal.ZERO)
        );
    }

    List<EventCountMetric> getAggregatedEventCountMetrics(Resource resource, TimeInterval interval,
            Duration chunkDuration);
}
