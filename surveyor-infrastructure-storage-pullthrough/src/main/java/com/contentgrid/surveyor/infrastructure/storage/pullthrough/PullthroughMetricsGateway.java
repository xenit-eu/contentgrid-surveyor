package com.contentgrid.surveyor.infrastructure.storage.pullthrough;

import com.contentgrid.surveyor.spi.ResourceDefinition;
import com.contentgrid.surveyor.spi.TimeInterval;
import com.contentgrid.surveyor.spi.config.FindCollectionConfigurationsSpiPort;
import com.contentgrid.surveyor.spi.resources.Metric;
import com.contentgrid.surveyor.spi.collector.MeasurementCollector;
import com.contentgrid.surveyor.spi.config.MetricCollectionConfig;
import com.contentgrid.surveyor.spi.storage.AggregateMeasurementsSpiPort;
import com.contentgrid.surveyor.spi.storage.Measurement;
import com.contentgrid.surveyor.spi.storage.aggregation.AggregationConfiguration;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;

@RequiredArgsConstructor
public class PullthroughMetricsGateway implements AggregateMeasurementsSpiPort {

    private final List<? extends MeasurementCollector> measurementCollectors;
    private final FindCollectionConfigurationsSpiPort findCollectionConfigurationsSpiPort;

    @Override
    public Flux<Measurement> findMeasurements(Metric metric, TimeInterval interval,
            AggregationConfiguration aggregationConfiguration) {
        return Flux.from(findMeasurements(metric.getResourceDefinition(), interval, aggregationConfiguration))
                .filter(measurement -> Objects.equals(measurement.getMetric(), metric));
    }

    @Override
    public Flux<Measurement> findMeasurements(ResourceDefinition resourceDefinition,
            TimeInterval interval,
            AggregationConfiguration aggregationConfiguration) {
        if (!aggregationConfiguration.isEmpty()) {
            throw new IllegalArgumentException("Pullthrough gateway can not aggregate metrics");
        }

        var collectedMetrics = Flux.fromIterable(measurementCollectors)
                .flatMap(source -> Flux.fromIterable(
                                        findCollectionConfigurationsSpiPort.findConfigurationsFor(source.getSystemType())
                                )
                                .map(config -> new ConfigAndSource(config, source))
                )
                .flatMap(configAndSource -> {
                    var maybeDefinition = configAndSource.measurementCollector()
                            .resourceDefinition(configAndSource.config())
                            .filter(Predicate.isEqual(resourceDefinition));

                    if (maybeDefinition.isPresent()) {
                        return configAndSource.measurementCollector()
                                .collectMeasurementsForBackfilling(configAndSource.config(), interval);
                    }
                    return Flux.empty();
                });

        return collectedMetrics
                .map(collected -> new Measurement(
                        collected.timeInterval(),
                        collected.resourceDefinition().createMetric(collected.resourceId(), collected.tags()),
                        collected.value()
                ));
    }

    private record ConfigAndSource(
            MetricCollectionConfig config,
            MeasurementCollector measurementCollector
    ) {

    }

}
