package com.contentgrid.surveyor.infrastructure.storage.memory;

import static org.assertj.core.api.Assertions.assertThat;

import com.contentgrid.surveyor.spi.storage.AggregateGaugeMetricSpiPort.AggregationConfiguration;
import com.contentgrid.surveyor.spi.storage.GaugeMetric;
import com.contentgrid.surveyor.spi.storage.Resource;
import com.contentgrid.surveyor.spi.storage.TimeInterval;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;

class MetricsGatewayTest {
    @Test
    void storeAndAggregateGauges() {
        var gateway = new MetricsGateway();
        var resource = new Resource("x", "y", "z");

        var startTime = Instant.parse("2020-01-01T00:00:00Z");
        var measureInterval = Duration.of(1, ChronoUnit.MINUTES);

        for(int i = 0; i < 600; i++) {
            gateway.storeGaugeMetric(new GaugeMetric(
                    startTime.plus(measureInterval.multipliedBy(i)),
                    resource,
                    BigInteger.valueOf(i)
            ));
        }

        var aggregation = gateway.getAggregatedGaugeMetric(resource, new TimeInterval(
                Instant.parse("2020-01-01T00:10:00Z"),
                Instant.parse("2020-01-01T02:10:00Z")
        ), AggregationConfiguration.builder()
                .averagingWindow(Duration.of(1, ChronoUnit.HOURS))
                .build());

        // averaged over 1 hour: values 10 -> 69 == 39.5
        //                       values 70 -> 129 == 99.5

        assertThat(aggregation.getResource()).isEqualTo(resource);
        assertThat(aggregation.getAggregate()).isEqualTo(BigDecimal.valueOf(139.0));

        // With an offset less than the measurement interval
        aggregation = gateway.getAggregatedGaugeMetric(resource, new TimeInterval(
                Instant.parse("2020-01-01T00:09:30Z"),
                Instant.parse("2020-01-01T02:09:30Z")
        ), AggregationConfiguration.builder()
                .averagingWindow(Duration.of(1, ChronoUnit.HOURS))
                .build());

        assertThat(aggregation.getAggregate()).isEqualTo(BigDecimal.valueOf(139.0));
    }

}