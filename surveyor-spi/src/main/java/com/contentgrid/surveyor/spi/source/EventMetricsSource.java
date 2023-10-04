package com.contentgrid.surveyor.spi.source;

import com.contentgrid.surveyor.spi.ResourceDefinition;
import com.contentgrid.surveyor.spi.TimeInterval;
import java.time.Instant;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.experimental.StandardException;

public interface EventMetricsSource {

    Optional<ResourceDefinition> resourceDefinition(MetricCollectionConfig config);

    Stream<CollectedMetric> collectMetrics(MetricCollectionConfig config, Instant referenceTime)
            throws CollectionFailedException;

    Stream<CollectedMetric> collectMetricsForBackfilling(MetricCollectionConfig config, TimeInterval interval)
            throws CollectionFailedException;

    default Stream<CollectedMetric> collectMetricsForBackfilling(MetricCollectionConfig config, Instant startTime,
            Instant endTime)
            throws CollectionFailedException {
        return collectMetricsForBackfilling(config, TimeInterval.between(startTime, endTime));
    }

    @StandardException
    class CollectionFailedException extends Exception {

    }
}
