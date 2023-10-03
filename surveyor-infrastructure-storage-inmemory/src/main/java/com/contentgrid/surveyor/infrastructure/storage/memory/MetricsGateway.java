package com.contentgrid.surveyor.infrastructure.storage.memory;

import com.contentgrid.surveyor.spi.ResourceDefinition;
import com.contentgrid.surveyor.spi.TimeInterval;
import com.contentgrid.surveyor.spi.storage.AggregateEventCountMetricSpiPort;
import com.contentgrid.surveyor.spi.storage.AggregateGaugeMetricSpiPort;
import com.contentgrid.surveyor.spi.storage.AggregatedGaugeMetric;
import com.contentgrid.surveyor.spi.storage.EventCountMetric;
import com.contentgrid.surveyor.spi.storage.GaugeMetric;
import com.contentgrid.surveyor.spi.storage.LastEventCountMetricSpiPort;
import com.contentgrid.surveyor.spi.storage.Resource;
import com.contentgrid.surveyor.spi.storage.StoreEventCountMetricSpiPort;
import com.contentgrid.surveyor.spi.storage.StoreGaugeMetricSpiPort;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public class MetricsGateway implements StoreGaugeMetricSpiPort, StoreEventCountMetricSpiPort,
        AggregateEventCountMetricSpiPort,
        AggregateGaugeMetricSpiPort,
        LastEventCountMetricSpiPort {

    private final List<EventCountMetric> eventCountMetrics = new LinkedList<>();
    private final List<GaugeMetric> gaugeMetrics = new LinkedList<>();

    @Override
    public List<EventCountMetric> getAggregatedEventCountMetrics(Resource resource, TimeInterval interval,
            Duration chunkDuration) {

        return interval.chunkedBy(chunkDuration)
                .flatMap(metricTimeInterval -> {
                    return eventCountMetrics.stream()
                            .filter(m -> Objects.equals(m.getResource(), resource))
                            .filter(m -> {
                                var contains = metricTimeInterval.contains(m.getMeasureInterval());

                                return contains.isContained() && !contains.isPartiallyAfter();
                            })
                            .reduce((m1, m2) -> new EventCountMetric(
                                    merge(m1.getMeasureInterval(), m2.getMeasureInterval()),
                                    m1.getResource(),
                                    m1.getValue().add(m2.getValue())
                            ))
                            .stream();
                })
                .toList();
    }

    @Override
    public List<AggregatedGaugeMetric> getAggregatedGaugeMetrics(Resource resource, TimeInterval interval,
            AggregationConfiguration aggregation, Duration chunkDuration) {
        var filteredMetrics = gaugeMetrics.stream()
                .filter(m -> Objects.equals(m.getResource(), resource))
                .toList();
        return interval.chunkedBy(chunkDuration)
                .flatMap(metricTimeInterval -> {
                    return metricTimeInterval.chunkedBy(aggregation.getAveragingWindow())
                            .flatMap(averagingTimeInterval -> {
                                var minTimeStamp = Instant.MAX;
                                var maxTimeStamp = Instant.MIN;
                                List<BigInteger> values = new ArrayList<>();
                                for (GaugeMetric metric : filteredMetrics) {
                                    if (averagingTimeInterval.contains(metric.getMeasureTime())) {
                                        minTimeStamp = minTimeStamp.isBefore(metric.getMeasureTime()) ? minTimeStamp
                                                : metric.getMeasureTime();
                                        maxTimeStamp = maxTimeStamp.isAfter(metric.getMeasureTime()) ? maxTimeStamp
                                                : metric.getMeasureTime();
                                        values.add(metric.getValue());
                                    }
                                }

                                if (values.isEmpty()) {
                                    return Stream.empty();
                                }
                                var total = values.stream()
                                        .map(BigDecimal::new)
                                        .reduce(BigDecimal::add).orElseThrow()
                                        .divide(BigDecimal.valueOf(values.size()));

                                return Stream.of(new AggregatedGaugeMetric(
                                        TimeInterval.between(minTimeStamp, maxTimeStamp),
                                        resource,
                                        total
                                ));
                            })
                            .reduce((m1, m2) -> new AggregatedGaugeMetric(
                                    merge(m1.getInterval(), m2.getInterval()),
                                    m1.getResource(),
                                    m1.getAggregate().add(m2.getAggregate())
                            ))
                            .stream();
                })
                .toList();
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
    public void storeGaugeMetric(GaugeMetric gaugeMetric) {
        gaugeMetrics.add(gaugeMetric);
    }

    @Override
    public Optional<TimeInterval> getLastEventCountMetricInterval(ResourceDefinition resourceDefinition) {
        return eventCountMetrics.stream()
                .filter(m -> Objects.equals(m.getResource().getDefinition(), resourceDefinition))
                .map(EventCountMetric::getMeasureInterval)
                .reduce((a, b) -> a.getEndTime().isBefore(b.getEndTime()) ? b : a);
    }
}
