package com.contentgrid.surveyor.infrastructure.storage.memory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.in;

import com.contentgrid.surveyor.spi.ResourceDefinition;
import com.contentgrid.surveyor.spi.TimeInterval;
import com.contentgrid.surveyor.spi.storage.AggregateEventCountMetricSpiPort.GroupingConfiguration;
import com.contentgrid.surveyor.spi.storage.AggregateEventCountMetricSpiPort.GroupingConfiguration.GroupOperation;
import com.contentgrid.surveyor.spi.storage.EventCountMetric;
import com.contentgrid.surveyor.spi.storage.Resource;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;

class MetricsGatewayTest {

    @Test
    void storeAndAggregateCounts() {
        var gateway = new MetricsGateway();
        var resource = new Resource(new ResourceDefinition("a", "x", "y"), "y");

        var startTime = Instant.parse("2020-01-01T00:00:00Z");
        var measureInterval = Duration.of(1, ChronoUnit.MINUTES);
        var interval = TimeInterval.after(startTime, measureInterval);

        for (int i = 0; i < 600; i++) {
            gateway.storeEventMetric(new EventCountMetric(
                    interval,
                    resource,
                    BigDecimal.valueOf(i)
            ));
            interval = interval.nextInterval();
        }

        var aggregation = gateway.getAggregatedEventCountMetric(resource, TimeInterval.between(
                        Instant.parse("2020-01-01T00:10:00Z"),
                        Instant.parse("2020-01-01T02:10:00Z")
                ), GroupingConfiguration.builder()
                        .groupInterval(Duration.of(1, ChronoUnit.HOURS))
                        .operation(GroupOperation.AVERAGE)
                        .build()
        );

        // averaged over 1 hour: values 10 -> 69 == 39.5
        //                       values 70 -> 129 == 99.5

        assertThat(aggregation.getResource()).isEqualTo(resource);
        assertThat(aggregation.getValue()).isEqualTo(BigDecimal.valueOf(39.5 + 99.5));

        // With an offset less than the measurement interval
        // The interval [00:09:00,00:10:00) is taken into account, but the interval [01:09:00,01:10:00( is not taken into account
        // averaged over 1 hour: values 9 -> 68 == 38.5
        //                       values 69 -> 128 == 98.5
        aggregation = gateway.getAggregatedEventCountMetric(resource, TimeInterval.between(
                Instant.parse("2020-01-01T00:09:30Z"),
                Instant.parse("2020-01-01T02:09:30Z")
        ), GroupingConfiguration.builder()
                .groupInterval(Duration.of(1, ChronoUnit.HOURS))
                .operation(GroupOperation.AVERAGE)
                .build());

        assertThat(aggregation.getValue()).isEqualTo(BigDecimal.valueOf(38.5 + 98.5));
    }

}