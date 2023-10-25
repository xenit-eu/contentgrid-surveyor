package com.contentgrid.surveyor.infrastructure.storage.memory;

import com.contentgrid.surveyor.spi.ResourceDefinition;
import com.contentgrid.surveyor.spi.TimeInterval;
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
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

public class InMemoryMetricsGateway implements StoreEventCountMetricSpiPort,
        AggregateEventCountMetricSpiPort,
        LastEventCountMetricSpiPort {

    private final Map<Resource, List<EventCountMetric>> eventCountMetrics = new HashMap<>();
    private final Map<ResourceDefinition, TimeInterval> lastApplied = new HashMap<>();

    @Override
    public Publisher<EventCountMetric> findEventCountMetrics(Resource resource, TimeInterval interval,
            AggregationConfiguration aggregationConfiguration) {
        var metrics = getMetricsInInterval(resource, interval);
        return Flux.fromStream(bucketEventCountMetricsRecursive(metrics, resource, interval, aggregationConfiguration));
    }

    @Override
    public Publisher<EventCountMetric> findEventCountMetrics(ResourceDefinition resourceDefinition,
            TimeInterval interval,
            AggregationConfiguration aggregationConfiguration) {

        var resources = eventCountMetrics.keySet().stream()
                .filter(resource -> Objects.equals(resource.getDefinition(), resourceDefinition));

        return Flux.fromStream(resources
                .flatMap(
                        resource -> bucketEventCountMetricsRecursive(
                                getMetricsInInterval(resource, interval),
                                resource,
                                interval,
                                aggregationConfiguration
                        )
                ));
    }

    private Stream<EventCountMetric> bucketEventCountMetricsRecursive(
            Stream<EventCountMetric> metrics,
            Resource resource,
            TimeInterval timeInterval,
            AggregationConfiguration aggregationConfiguration
    ) {
        if (aggregationConfiguration.isEmpty()) {
            return metrics;
        }
        var split = aggregationConfiguration.splitLeft();
        var firstOp = split.operation();

        var aggregatedMetrics = firstOp.perform(bucketingOperation -> {
                    return metrics
                            .collect(Collectors.groupingBy(
                                    metric -> bucketingKey(bucketingOperation.bucket(),
                                            metric.getMeasureInterval().getEndTime(), timeInterval.getStartTime()),
                                    groupingCollector(resource, bucketingOperation.operation())
                            ))
                            .values()
                            .stream()
                            .flatMap(Optional::stream);
                },
                finishingOperation -> {
                    return metrics.collect(groupingCollector(resource, finishingOperation.operation())).stream();
                }
        );

        return bucketEventCountMetricsRecursive(
                aggregatedMetrics,
                resource,
                timeInterval,
                split
        );
    }

    private long bucketingKey(Duration bucket, Instant time, Instant reference) {
        var fromReference = Duration.between(reference, time);

        var bucketKey = fromReference.dividedBy(bucket);
        // division is not exactly right, as we want the last item to still be part of the bucket
        if (Objects.equals(fromReference, bucket.multipliedBy(bucketKey))) {
            bucketKey -= 1;
        }
        return bucketKey;
    }

    private Collector<EventCountMetric, ?, Optional<EventCountMetric>> groupingCollector(Resource resource,
            AggregationOperation aggregationOperation) {
        return Collectors.teeing(
                Collectors.mapping(EventCountMetric::getMeasureInterval,
                        Collectors.reducing(InMemoryMetricsGateway::merge)),
                Collectors.mapping(EventCountMetric::getValue, collectorFor(aggregationOperation)),
                (maybeMergedInterval, maybeValue) -> maybeMergedInterval.flatMap(
                        mergedInterval -> maybeValue.map(
                                value -> new EventCountMetric(mergedInterval, resource,
                                        value)))
        );
    }

    private Stream<EventCountMetric> getMetricsInInterval(Resource resource, TimeInterval groupInterval) {
        return List.copyOf(eventCountMetrics.getOrDefault(resource, List.of())).stream()
                .filter(m -> Objects.equals(m.getResource(), resource))
                .filter(m -> {
                    var endTime = m.getMeasureInterval().getEndTime();
                    return groupInterval.getStartTime().isBefore(endTime) && (
                            groupInterval.getEndTime().isAfter(endTime) ||
                                    groupInterval.getEndTime().equals(endTime)
                    );
                });
    }

    private Collector<BigDecimal, ?, Optional<BigDecimal>> collectorFor(AggregationOperation operation) {
        return switch (operation) {
            case AVERAGE -> Collector.of(
                    BigDecimalAverageAccumulator::new,
                    BigDecimalAverageAccumulator::add,
                    BigDecimalAverageAccumulator::merge,
                    BigDecimalAverageAccumulator::getAverage
            );
            case MAX -> Collectors.maxBy(BigDecimal::compareTo);
            case MIN -> Collectors.minBy(BigDecimal::compareTo);
            case SUM -> Collectors.reducing(BigDecimal::add);
        };
    }

    private static TimeInterval merge(TimeInterval ti1, TimeInterval ti2) {
        Instant startTime;
        if (ti1.getStartTime().isBefore(ti2.getStartTime())) {
            startTime = ti1.getStartTime();
        } else {
            startTime = ti2.getStartTime();
        }

        Instant endTime;
        if (ti1.getEndTime().isBefore(ti2.getEndTime())) {
            endTime = ti2.getEndTime();
        } else {
            endTime = ti1.getEndTime();
        }

        return TimeInterval.between(startTime, endTime);
    }

    @Override
    public void storeEventMetric(EventCountMetric metric) {
        eventCountMetrics.computeIfAbsent(metric.getResource(), _key -> new LinkedList<>()).add(metric);
        lastApplied.compute(metric.getResource().getDefinition(), (_key, interval) -> {
            if (interval == null || interval.getEndTime().isBefore(metric.getMeasureInterval().getEndTime())) {
                return metric.getMeasureInterval();
            }
            return interval;
        });
    }

    @Override
    public Optional<TimeInterval> getLastEventCountMetricInterval(ResourceDefinition resourceDefinition) {
        return Optional.ofNullable(lastApplied.get(resourceDefinition));
    }

    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    private static class BigDecimalAverageAccumulator {


        private BigInteger count;
        private BigDecimal tally;

        public BigDecimalAverageAccumulator() {
            this(BigInteger.ZERO, BigDecimal.ZERO);
        }

        public static BigDecimalAverageAccumulator of(BigDecimal bigDecimal) {
            return new BigDecimalAverageAccumulator(BigInteger.ONE, bigDecimal);
        }

        public void add(BigDecimal bigDecimal) {
            count = count.add(BigInteger.ONE);
            tally = tally.add(bigDecimal);
        }

        public BigDecimalAverageAccumulator merge(BigDecimalAverageAccumulator accumulator) {
            return new BigDecimalAverageAccumulator(count.add(accumulator.count), tally.add(accumulator.tally));
        }

        public Optional<BigDecimal> getAverage() {
            if (Objects.equals(count, BigInteger.ZERO)) {
                return Optional.empty();
            }
            return Optional.of(tally.divide(new BigDecimal(count),
                    new MathContext(tally.precision() + 1, RoundingMode.HALF_EVEN)));
        }
    }
}
