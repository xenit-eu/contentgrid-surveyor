package com.contentgrid.surveyor.infrastructure.storage.pullthrough;

import com.contentgrid.surveyor.spi.ResourceDefinition;
import com.contentgrid.surveyor.spi.TimeInterval;
import com.contentgrid.surveyor.spi.config.FindCollectionConfigurationsSpiPort;
import com.contentgrid.surveyor.spi.source.EventMetricsSource;
import com.contentgrid.surveyor.spi.source.EventMetricsSource.CollectionFailedException;
import com.contentgrid.surveyor.spi.config.MetricCollectionConfig;
import com.contentgrid.surveyor.spi.storage.AggregateEventCountMetricSpiPort;
import com.contentgrid.surveyor.spi.storage.EventCountMetric;
import com.contentgrid.surveyor.spi.storage.Resource;
import com.contentgrid.surveyor.spi.storage.aggregation.AggregationConfiguration;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import lombok.RequiredArgsConstructor;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

@RequiredArgsConstructor
public class PullthroughMetricsGateway implements AggregateEventCountMetricSpiPort {

    private final List<? extends EventMetricsSource> metricSources;
    private final FindCollectionConfigurationsSpiPort findCollectionConfigurationsSpiPort;

    @Override
    public Publisher<EventCountMetric> findEventCountMetrics(Resource resource, TimeInterval interval,
            AggregationConfiguration aggregationConfiguration) {
        return Flux.from(findEventCountMetrics(resource.getDefinition(), interval, aggregationConfiguration))
                .filter(metric -> Objects.equals(metric.getResource(), resource));
    }

    @Override
    public Publisher<EventCountMetric> findEventCountMetrics(ResourceDefinition resourceDefinition,
            TimeInterval interval,
            AggregationConfiguration aggregationConfiguration) {
        if (!aggregationConfiguration.isEmpty()) {
            throw new IllegalArgumentException("Pullthrough gateway can not aggregate metrics");
        }

        var collectedMetrics = Flux.fromIterable(metricSources)
                .flatMap(source -> Flux.fromIterable(
                                        findCollectionConfigurationsSpiPort.findConfigurationsFor(source.getSystemType())
                                )
                                .map(config -> new ConfigAndSource(config, source))
                )
                .flatMap(configAndSource -> {
                    var maybeDefinition = configAndSource.metricsSource()
                            .resourceDefinition(configAndSource.config())
                            .filter(Predicate.isEqual(resourceDefinition));

                    if (maybeDefinition.isPresent()) {
                        try {
                            return configAndSource.metricsSource()
                                    .collectMetricsForBackfilling(configAndSource.config(), interval);
                        } catch (CollectionFailedException e) {
                            return Flux.error(e);
                        }
                    }
                    return Flux.empty();
                });

        return collectedMetrics
                .map(collected -> new EventCountMetric(
                        collected.timeInterval(),
                        new Resource(collected.resourceDefinition(), collected.resourceId()),
                        collected.value()
                ));
    }

    private record ConfigAndSource(
            MetricCollectionConfig config,
            EventMetricsSource metricsSource
    ) {

    }

}
