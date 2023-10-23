package com.contentgrid.surveyor.spi.source;

import com.contentgrid.surveyor.spi.ResourceDefinition;
import com.contentgrid.surveyor.spi.TimeInterval;
import java.time.Instant;
import java.util.Optional;
import lombok.experimental.StandardException;
import org.reactivestreams.Publisher;

public interface EventMetricsSource {

    Optional<ResourceDefinition> resourceDefinition(MetricCollectionConfig config);

    Publisher<CollectedMetric> collectMetrics(MetricCollectionConfig config, Instant startedAt)
            throws CollectionFailedException;

    Publisher<CollectedMetric> collectMetricsForBackfilling(MetricCollectionConfig config, TimeInterval interval)
            throws CollectionFailedException;

    @StandardException
    class CollectionFailedException extends Exception {

    }
}
