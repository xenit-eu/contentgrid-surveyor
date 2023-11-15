package com.contentgrid.surveyor.spi.collector;

import com.contentgrid.surveyor.spi.MetricCollectorSystemType;
import com.contentgrid.surveyor.spi.ResourceDefinition;
import com.contentgrid.surveyor.spi.TimeInterval;
import com.contentgrid.surveyor.spi.config.MetricCollectionConfig;
import java.time.Instant;
import java.util.Optional;
import lombok.experimental.StandardException;
import org.reactivestreams.Publisher;

public interface MeasurementCollector {

    MetricCollectorSystemType getSystemType();

    Optional<ResourceDefinition> resourceDefinition(MetricCollectionConfig config);

    Publisher<CollectedMeasurement> collectMeasurements(MetricCollectionConfig config, Instant startedAt);

    Publisher<CollectedMeasurement> collectMeasurementsForBackfilling(MetricCollectionConfig config,
            TimeInterval interval);

    @StandardException
    class CollectionFailedException extends Exception {

    }
}
