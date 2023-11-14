package com.contentgrid.surveyor.infrastructure.storage.memory;

import com.contentgrid.surveyor.spi.ResourceDefinition;
import com.contentgrid.surveyor.spi.TimeInterval;
import com.contentgrid.surveyor.spi.resources.CreateMetricSpiPort;
import com.contentgrid.surveyor.spi.resources.Metric;
import com.contentgrid.surveyor.spi.storage.AggregateMeasurementsSpiPort;
import com.contentgrid.surveyor.spi.storage.Measurement;
import com.contentgrid.surveyor.spi.storage.LastMeasurementSpiPort;
import com.contentgrid.surveyor.spi.storage.StoreMeasurementSpiPort;
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
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class InMemoryMeasurementGateway implements StoreMeasurementSpiPort,
        AggregateMeasurementsSpiPort,
        LastMeasurementSpiPort {

    private final Map<Metric, List<Measurement>> eventCountMetrics = new HashMap<>();
    private final Map<ResourceDefinition, TimeInterval> lastApplied = new HashMap<>();
    private final CreateMetricSpiPort createMetricSpiPort;

    public InMemoryMeasurementGateway() {
        this(resource -> {
            return Mono.empty();
        });
    }

    @Override
    public Flux<Measurement> findMeasurements(Metric metric, TimeInterval interval,
            AggregationConfiguration aggregationConfiguration) {
        var metrics = getMetricsInInterval(metric, interval);
        return Flux.fromStream(bucketEventCountMetricsRecursive(metrics, metric, interval, aggregationConfiguration));
    }

    @Override
    public Flux<Measurement> findMeasurements(ResourceDefinition resourceDefinition,
            TimeInterval interval,
            AggregationConfiguration aggregationConfiguration) {

        var metrics = eventCountMetrics.keySet().stream()
                .filter(resource -> Objects.equals(resource.getResourceDefinition(), resourceDefinition));

        return Flux.fromStream(metrics
                .flatMap(
                        metric -> bucketEventCountMetricsRecursive(
                                getMetricsInInterval(metric, interval),
                                metric,
                                interval,
                                aggregationConfiguration
                        )
                ));
    }

    private Stream<Measurement> bucketEventCountMetricsRecursive(
            Stream<Measurement> measurements,
            Metric metric,
            TimeInterval timeInterval,
            AggregationConfiguration aggregationConfiguration
    ) {
        if (aggregationConfiguration.isEmpty()) {
            return measurements;
        }
        var split = aggregationConfiguration.splitLeft();
        var firstOp = split.operation();

        var aggregatedMetrics = firstOp.perform(bucketingOperation -> {
                    return measurements
                            .collect(Collectors.groupingBy(
                                    measurement -> bucketingKey(bucketingOperation.bucket(),
                                            measurement.getMeasureInterval().getEndTime(), timeInterval.getStartTime()),
                                    groupingCollector(metric, bucketingOperation.operation())
                            ))
                            .values()
                            .stream()
                            .flatMap(Optional::stream);
                },
                finishingOperation -> {
                    return measurements.collect(groupingCollector(metric, finishingOperation.operation())).stream();
                }
        );

        return bucketEventCountMetricsRecursive(
                aggregatedMetrics,
                metric,
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

    private Collector<Measurement, ?, Optional<Measurement>> groupingCollector(Metric metric,
            AggregationOperation aggregationOperation) {
        return Collectors.teeing(
                Collectors.mapping(Measurement::getMeasureInterval,
                        Collectors.reducing(InMemoryMeasurementGateway::merge)),
                Collectors.mapping(Measurement::getValue, collectorFor(aggregationOperation)),
                (maybeMergedInterval, maybeValue) -> maybeMergedInterval.flatMap(
                        mergedInterval -> maybeValue.map(
                                value -> new Measurement(mergedInterval, metric,
                                        value)))
        );
    }

    private Stream<Measurement> getMetricsInInterval(Metric metric, TimeInterval groupInterval) {
        return List.copyOf(eventCountMetrics.getOrDefault(metric, List.of())).stream()
                .filter(m -> Objects.equals(m.getMetric(), metric))
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
    public Mono<Void> storeMeasurement(Measurement measurement) {
        return createMetricSpiPort.createMetric(measurement.getMetric())
                .then(Mono.fromRunnable(() -> {
                    eventCountMetrics.computeIfAbsent(measurement.getMetric(), _key -> new LinkedList<>())
                            .add(measurement);
                    lastApplied.compute(measurement.getMetric().getResourceDefinition(), (_key, interval) -> {
                        if (interval == null || interval.getEndTime()
                                .isBefore(measurement.getMeasureInterval().getEndTime())) {
                            return measurement.getMeasureInterval();
                        }
                        return interval;
                    });
                }));
    }

    @Override
    public Mono<TimeInterval> getLastMeasurementInterval(ResourceDefinition resourceDefinition) {
        return Mono.justOrEmpty(lastApplied.get(resourceDefinition));
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
