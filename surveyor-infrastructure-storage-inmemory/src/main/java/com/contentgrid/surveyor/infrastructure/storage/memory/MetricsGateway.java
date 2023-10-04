package com.contentgrid.surveyor.infrastructure.storage.memory;

import com.contentgrid.surveyor.spi.ResourceDefinition;
import com.contentgrid.surveyor.spi.TimeInterval;
import com.contentgrid.surveyor.spi.storage.AggregateEventCountMetricSpiPort;
import com.contentgrid.surveyor.spi.storage.AggregateEventCountMetricSpiPort.GroupingConfiguration.GroupOperation;
import com.contentgrid.surveyor.spi.storage.EventCountMetric;
import com.contentgrid.surveyor.spi.storage.LastEventCountMetricSpiPort;
import com.contentgrid.surveyor.spi.storage.Resource;
import com.contentgrid.surveyor.spi.storage.StoreEventCountMetricSpiPort;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

public class MetricsGateway implements StoreEventCountMetricSpiPort,
        AggregateEventCountMetricSpiPort,
        LastEventCountMetricSpiPort {

    private final List<EventCountMetric> eventCountMetrics = new LinkedList<>();

    @Override
    public List<EventCountMetric> getAggregatedEventCountMetrics(Resource resource, TimeInterval interval,
            Duration chunkDuration, GroupingConfiguration groupingConfiguration) {

        return interval.chunkedBy(chunkDuration)
                .flatMap(metricTimeInterval -> {
                    return metricTimeInterval.chunkedBy(groupingConfiguration.groupInterval())
                            .flatMap(groupInterval -> groupedMetrics(resource, groupingConfiguration.operation(),
                                    groupInterval))
                            .reduce((m1, m2) -> new EventCountMetric(
                                    merge(m1.getMeasureInterval(), m2.getMeasureInterval()),
                                    m1.getResource(),
                                    m1.getValue().add(m2.getValue())
                            ))
                            .stream();
                })
                .toList();
    }

    private Stream<EventCountMetric> groupedMetrics(Resource resource, GroupOperation groupOperation,
            TimeInterval groupInterval) {
        return getMetricsInInterval(resource, groupInterval)
                .collect(Collectors.teeing(
                        Collectors.mapping(EventCountMetric::getMeasureInterval,
                                Collectors.reducing(MetricsGateway::merge)),
                        Collectors.mapping(EventCountMetric::getValue, collectorFor(groupOperation)),
                        (maybeMergedInterval, maybeValue) -> maybeMergedInterval.flatMap(
                                mergedInterval -> maybeValue.map(
                                        value -> new EventCountMetric(mergedInterval, resource,
                                                value)))
                ))
                .stream();
    }

    private Stream<EventCountMetric> getMetricsInInterval(Resource resource, TimeInterval groupInterval) {
        return eventCountMetrics.stream()
                .filter(m -> Objects.equals(m.getResource(), resource))
                .filter(m -> {
                    var contains = groupInterval.contains(m.getMeasureInterval());

                    return contains.isContained() || (contains.isPartiallyBefore()
                            && !contains.isPartiallyAfter());
                });
    }

    private Collector<BigDecimal, ?, Optional<BigDecimal>> collectorFor(GroupOperation operation) {
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
        eventCountMetrics.add(metric);
    }

    @Override
    public Optional<TimeInterval> getLastEventCountMetricInterval(ResourceDefinition resourceDefinition) {
        return eventCountMetrics.stream()
                .filter(m -> Objects.equals(m.getResource().getDefinition(), resourceDefinition))
                .map(EventCountMetric::getMeasureInterval)
                .reduce((a, b) -> a.getEndTime().isBefore(b.getEndTime()) ? b : a);
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
