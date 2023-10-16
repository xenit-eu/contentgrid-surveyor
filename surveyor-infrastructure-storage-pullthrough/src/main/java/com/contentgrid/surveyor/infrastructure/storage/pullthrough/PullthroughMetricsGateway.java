package com.contentgrid.surveyor.infrastructure.storage.pullthrough;

import com.contentgrid.surveyor.spi.ResourceDefinition;
import com.contentgrid.surveyor.spi.TimeInterval;
import com.contentgrid.surveyor.spi.source.CollectedMetric;
import com.contentgrid.surveyor.spi.source.EventMetricsSource;
import com.contentgrid.surveyor.spi.source.EventMetricsSource.CollectionFailedException;
import com.contentgrid.surveyor.spi.source.MetricCollectionConfig;
import com.contentgrid.surveyor.spi.storage.AggregateEventCountMetricSpiPort;
import com.contentgrid.surveyor.spi.storage.EventCountMetric;
import com.contentgrid.surveyor.spi.storage.LastEventCountMetricSpiPort;
import com.contentgrid.surveyor.spi.storage.Resource;
import com.contentgrid.surveyor.spi.storage.StoreEventCountMetricSpiPort;
import com.contentgrid.surveyor.spi.storage.aggregation.AggregationConfiguration;
import com.contentgrid.surveyor.spi.storage.aggregation.AggregationOperation;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

@RequiredArgsConstructor
public class PullthroughMetricsGateway implements AggregateEventCountMetricSpiPort {

    private final List<? extends EventMetricsSource> metricSources;
    private final List<MetricCollectionConfig> collectionConfigs;

    @Override
    public List<EventCountMetric> findEventCountMetrics(Resource resource, TimeInterval interval,
            AggregationConfiguration aggregationConfiguration) {
        return findEventCountMetrics(resource.getDefinition(), interval, aggregationConfiguration).stream()
                .filter(metric -> Objects.equals(metric.getResource(), resource))
                .toList();

    }

    @Override
    @SneakyThrows(CollectionFailedException.class)
    public List<EventCountMetric> findEventCountMetrics(ResourceDefinition resourceDefinition, TimeInterval interval,
            AggregationConfiguration aggregationConfiguration) {
        if (!aggregationConfiguration.isEmpty()) {
            throw new IllegalArgumentException("Pullthrough gateway can not aggregate metrics");
        }

        Stream.Builder<Stream<CollectedMetric>> allMetrics = Stream.builder();

        for (MetricCollectionConfig collectionConfig : collectionConfigs) {
            for (EventMetricsSource metricSource : metricSources) {
                var maybeDefinition = metricSource.resourceDefinition(collectionConfig)
                        .filter(Predicate.isEqual(resourceDefinition));

                if (maybeDefinition.isPresent()) {
                    allMetrics.add(metricSource.collectMetricsForBackfilling(collectionConfig, interval));
                }
            }
        }

        return allMetrics.build()
                .flatMap(Function.identity())
                .map(collected -> new EventCountMetric(
                        collected.timeInterval(),
                        new Resource(collected.resourceDefinition(), collected.resourceId()),
                        collected.value()
                ))
                .toList();
    }

}
