package com.contentgrid.surveyor.spi.source;

import com.contentgrid.surveyor.spi.MetricSourceSystemType;
import com.contentgrid.surveyor.spi.ResourceDefinition;
import com.contentgrid.surveyor.spi.TimeInterval;
import com.contentgrid.surveyor.spi.config.MetricCollectionConfig;
import java.time.Instant;
import java.util.Optional;
import lombok.experimental.StandardException;
import org.reactivestreams.Publisher;

public interface EventMetricsSource {

    MetricSourceSystemType getSystemType();

    Optional<ResourceDefinition> resourceDefinition(MetricCollectionConfig config);

    Publisher<CollectedMetric> collectMetrics(MetricCollectionConfig config, Instant startedAt)
            throws CollectionFailedException;

    Publisher<CollectedMetric> collectMetricsForBackfilling(MetricCollectionConfig config, TimeInterval interval)
            throws CollectionFailedException;

    @StandardException
    class CollectionFailedException extends Exception {

    }
}
