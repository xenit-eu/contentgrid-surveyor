package com.contentgrid.surveyor.infrastructure.storage.tests;

import static org.assertj.core.api.Assertions.assertThat;

import com.contentgrid.surveyor.spi.TimeInterval;
import com.contentgrid.surveyor.spi.resources.Metric;
import com.contentgrid.surveyor.spi.resources.ResourceIdentity;
import com.contentgrid.surveyor.spi.storage.AggregateMeasurementsSpiPort;
import com.contentgrid.surveyor.spi.storage.LastMeasurementSpiPort;
import com.contentgrid.surveyor.spi.storage.Measurement;
import com.contentgrid.surveyor.spi.storage.StoreMeasurementSpiPort;
import com.contentgrid.surveyor.spi.storage.aggregation.AggregationConfiguration;
import com.contentgrid.surveyor.spi.storage.aggregation.AggregationOperation;
import com.contentgrid.surveyor.values.MetricName;
import com.contentgrid.surveyor.values.ResourceId;
import com.contentgrid.surveyor.values.ResourceType;
import com.contentgrid.surveyor.values.SourceName;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

public abstract class MetricsGatewayTest {

    protected abstract StoreMeasurementSpiPort getStoreEventCountMetricPort();

    protected abstract AggregateMeasurementsSpiPort getAggregateEventCountMetricPort();

    protected abstract LastMeasurementSpiPort getLastEventCountMetricPort();

    @Test
    void storeAndAggregateAveraging() {
        var store = getStoreEventCountMetricPort();
        var aggregate = getAggregateEventCountMetricPort();
        var metric = new Metric(
                new ResourceIdentity(
                        SourceName.of(UUID.randomUUID().toString()),
                        ResourceType.of("x"),
                        ResourceId.of("y")
                ),
                MetricName.of("y"),
                Map.of()
        );

        var startTime = Instant.parse("2020-01-01T00:00:00Z");
        var measureInterval = Duration.of(1, ChronoUnit.MINUTES);
        var interval = TimeInterval.after(startTime, measureInterval);

        for (int i = 0; i < 600; i++) {
            store.storeMeasurement(new Measurement(
                    interval,
                    metric,
                    BigDecimal.valueOf(i)
            )).block();
            interval = interval.nextInterval();
        }

        var aggregation = aggregate.getAggregatedMeasurements(metric, TimeInterval.between(
                        Instant.parse("2020-01-01T00:10:00Z"),
                        Instant.parse("2020-01-01T02:10:00Z")
                ), AggregationConfiguration.builder()
                        .thenBucket(Duration.ofHours(1), AggregationOperation.AVERAGE)
                        .finallyAggregate(AggregationOperation.SUM)
        ).block();

        // averaged over 1 hour: values 10 -> 69 == 39.5
        //                       values 70 -> 129 == 99.5

        assertThat(aggregation.getMetric()).isEqualTo(metric);
        assertThat(aggregation.getValue().stripTrailingZeros()).isEqualTo(
                BigDecimal.valueOf(39.5 + 99.5).stripTrailingZeros());

        // With an offset less than the measurement interval
        // The interval [00:09:00,00:10:00) is taken into account, but the interval [02:09:00,02:10:00( is not taken into account
        // averaged over 1 hour: values 9 -> 68 == 38.5
        //                       values 69 -> 128 == 98.5
        aggregation = aggregate.getAggregatedMeasurements(metric, TimeInterval.between(
                        Instant.parse("2020-01-01T00:09:30Z"),
                        Instant.parse("2020-01-01T02:09:30Z")
                ), AggregationConfiguration.builder()
                        .thenBucket(Duration.ofHours(1), AggregationOperation.AVERAGE)
                        .finallyAggregate(AggregationOperation.SUM)
        ).block();

        assertThat(aggregation.getValue().stripTrailingZeros()).isEqualTo(
                BigDecimal.valueOf(38.5 + 98.5).stripTrailingZeros());
    }

    @Test
    void storeAndAggregateCounts() {
        var store = getStoreEventCountMetricPort();
        var aggregate = getAggregateEventCountMetricPort();
        var metric = new Metric(
                new ResourceIdentity(
                        SourceName.of(UUID.randomUUID().toString()),
                        ResourceType.of("x"),
                        ResourceId.of("y")
                ),
                MetricName.of("y"),
                Map.of()
        );

        var startTime = Instant.parse("2020-01-01T00:00:00Z");
        var measureInterval = Duration.of(1, ChronoUnit.MINUTES);
        var interval = TimeInterval.after(startTime, measureInterval);

        for (int i = 0; i < 600; i++) {
            store.storeMeasurement(new Measurement(
                    interval,
                    metric,
                    BigDecimal.valueOf(8)
            )).block();
            interval = interval.nextInterval();
        }

        var aggregation = aggregate.getAggregatedMeasurements(metric, TimeInterval.between(
                        Instant.parse("2020-01-01T00:10:00Z"),
                        Instant.parse("2020-01-01T02:10:00Z")
                ), AggregationConfiguration.builder()
                        .thenBucket(Duration.ofHours(1), AggregationOperation.SUM)
                        .finallyAggregate(AggregationOperation.SUM)
        ).block();

        // summed over 1 hour: 8*60 == 480
        assertThat(aggregation.getValue().stripTrailingZeros()).isEqualTo(
                BigDecimal.valueOf(480 * 2).stripTrailingZeros());

        // When offset, the calculation remains the same
        aggregation = aggregate.getAggregatedMeasurements(metric, TimeInterval.between(
                        Instant.parse("2020-01-01T00:09:30Z"),
                        Instant.parse("2020-01-01T02:09:30Z")
                ), AggregationConfiguration.builder()
                        .thenBucket(Duration.ofHours(1), AggregationOperation.SUM)
                        .finallyAggregate(AggregationOperation.SUM)
        ).block();

        assertThat(aggregation.getValue().stripTrailingZeros()).isEqualTo(
                BigDecimal.valueOf(480 * 2).stripTrailingZeros());

        // When grouped with a smaller interval, the calculation remains the same
        aggregation = aggregate.getAggregatedMeasurements(metric, TimeInterval.between(
                        Instant.parse("2020-01-01T00:09:30Z"),
                        Instant.parse("2020-01-01T02:09:30Z")
                ), AggregationConfiguration.builder()
                        .thenBucket(Duration.ofHours(1), AggregationOperation.SUM)
                        .finallyAggregate(AggregationOperation.SUM)
        ).block();

        assertThat(aggregation.getValue().stripTrailingZeros()).isEqualTo(
                BigDecimal.valueOf(480 * 2).stripTrailingZeros());
    }

}