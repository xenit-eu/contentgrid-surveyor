package com.contentgrid.surveyor.spi.storage;

import java.math.BigInteger;
import java.time.Duration;
import java.util.List;

public interface AggregateEventCountMetricSpiPort {
    default EventCountMetric getAggregatedEventCountMetric(Resource resource, TimeInterval interval) {
        return Util.onlyValue(
                getAggregatedEventCountMetrics(resource, interval, Duration.between(interval.getStartTime(), interval.getEndTime())),
                () -> new EventCountMetric(interval, resource, BigInteger.ZERO)
        );
    }
    List<EventCountMetric> getAggregatedEventCountMetrics(Resource resource, TimeInterval interval, Duration chunkDuration);
}
