package com.contentgrid.surveyor.spi.source;

import com.contentgrid.surveyor.spi.MetricSourceSystemType;
import com.contentgrid.surveyor.spi.ResourceDefinition;
import com.contentgrid.surveyor.spi.TimeInterval;
import com.contentgrid.surveyor.spi.config.MeasurementCollectionConfig;
import java.time.Instant;
import java.util.Optional;
import lombok.experimental.StandardException;
import org.reactivestreams.Publisher;

public interface EventMetricsSource {

    MetricSourceSystemType getSystemType();

    Optional<ResourceDefinition> resourceDefinition(MeasurementCollectionConfig config);

    Publisher<CollectedMetric> collectMetrics(MeasurementCollectionConfig config, Instant startedAt);

    Publisher<CollectedMetric> collectMetricsForBackfilling(MeasurementCollectionConfig config, TimeInterval interval);

    @StandardException
    class CollectionFailedException extends Exception {

    }
}
