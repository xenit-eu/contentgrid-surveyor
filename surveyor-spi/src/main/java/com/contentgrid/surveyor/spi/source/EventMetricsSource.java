package com.contentgrid.surveyor.spi.source;

import java.time.Instant;
import java.util.stream.Stream;
import lombok.experimental.StandardException;

public interface EventMetricsSource {

    Stream<CollectedMetric> collectMetrics(Instant referenceTime) throws CollectionFailedException;

    Stream<CollectedMetric> collectMetricsForBackfilling(TimeInterval interval) throws CollectionFailedException;

    default Stream<CollectedMetric> collectMetricsForBackfilling(Instant startTime, Instant endTime)
            throws CollectionFailedException {
        return collectMetricsForBackfilling(TimeInterval.between(startTime, endTime));
    }

    @StandardException
    class CollectionFailedException extends Exception {

    }
}
