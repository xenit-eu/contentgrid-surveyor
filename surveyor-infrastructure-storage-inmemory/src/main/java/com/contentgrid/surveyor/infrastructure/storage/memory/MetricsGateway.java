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

public class MetricsGateway implements StoreEventCountMetricSpiPort,
        AggregateEventCountMetricSpiPort,
        LastEventCountMetricSpiPort {

    private final Map<Resource, List<EventCountMetric>> eventCountMetrics = new HashMap<>();
    private final Map<ResourceDefinition, TimeInterval> lastApplied = new HashMap<>();

    @Override
    public List<EventCountMetric> getAggregatedEventCountMetrics(Resource resource, TimeInterval interval,
            Duration chunkDuration, GroupingConfiguration groupingConfiguration) {

        return findEventCountMetrics(resource, interval, List.of(
                GroupingConfiguration.builder()
                        .groupInterval(chunkDuration)
                        .operation(GroupOperation.SUM)
                        .build(),
                groupingConfiguration
        ));
    }

    @Override
    public List<EventCountMetric> findEventCountMetrics(Resource resource, TimeInterval interval,
            List<GroupingConfiguration> groupingConfigurations) {
        return findEventCountMetricsRecursive(resource, interval, groupingConfigurations).toList();
    }

    public Stream<EventCountMetric> findEventCountMetricsRecursive(Resource resource, TimeInterval interval,
            List<GroupingConfiguration> groupingConfigurations) {
        if (groupingConfigurations.isEmpty()) {
            return getMetricsInInterval(resource, interval);
        }
        var firstGroup = groupingConfigurations.get(0);
        var otherGroups = groupingConfigurations.subList(1, groupingConfigurations.size());
        return interval.chunkedBy(firstGroup.groupInterval())
                .flatMap(groupInterval -> findEventCountMetricsRecursive(resource, groupInterval, otherGroups)
                        .collect(groupingCollector(resource, firstGroup.operation()))
                        .stream()
                );
    }

    private Collector<EventCountMetric, ?, Optional<EventCountMetric>> groupingCollector(Resource resource,
            GroupOperation groupOperation) {
        return Collectors.teeing(
                Collectors.mapping(EventCountMetric::getMeasureInterval,
                        Collectors.reducing(MetricsGateway::merge)),
                Collectors.mapping(EventCountMetric::getValue, collectorFor(groupOperation)),
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
