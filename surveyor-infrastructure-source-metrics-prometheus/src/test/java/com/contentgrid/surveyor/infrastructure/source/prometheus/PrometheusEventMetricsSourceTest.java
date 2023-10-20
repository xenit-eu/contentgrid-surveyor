package com.contentgrid.surveyor.infrastructure.source.prometheus;

import static org.assertj.core.api.Assertions.assertThat;

import com.contentgrid.surveyor.infrastructure.source.prometheus.test.FakeMetrics;
import com.contentgrid.surveyor.infrastructure.source.prometheus.test.FakeMetrics.MetricDefinition;
import com.contentgrid.surveyor.infrastructure.source.prometheus.test.PrometheusContainer;
import com.contentgrid.surveyor.spi.source.MetricCollectionConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class PrometheusEventMetricsSourceTest {

    @Container
    private static final PrometheusContainer PROMETHEUS = new PrometheusContainer()
            .withCommand("--storage.tsdb.retention.time=1y", "--config.file=/etc/prometheus/prometheus.yml");

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final Instant FAKE_METRICS_END = Instant.now().truncatedTo(ChronoUnit.DAYS)
            .minus(1, ChronoUnit.DAYS);
    private static final Instant FAKE_METRICS_START = FAKE_METRICS_END.minus(Duration.ofDays(80));
    private static final Duration FAKE_METRICS_INTERVAL = Duration.ofSeconds(30);

    @BeforeAll
    static void createFakeMetrics() throws IOException, InterruptedException {
        var fakeMetrics = new FakeMetrics(FAKE_METRICS_START, FAKE_METRICS_INTERVAL);

        var fixedMetricAbc = MetricDefinition.builder()
                .name("fixed_metric_1")
                .label("resource", "abc")
                .label("other", "def")
                .build();
        var fixedMetricXyz = MetricDefinition.builder()
                .name("fixed_metric_1")
                .label("resource", "xyz")
                .label("other", "def")
                .build();

        var growingMetricAbc = MetricDefinition.builder()
                .name("growing_metric_1")
                .label("resource", "abc")
                .build();

        var growingMetricXyz = MetricDefinition.builder()
                .name("growing_metric_1")
                .label("resource", "xyz")
                .build();

        fakeMetrics.recordAllUntil(fixedMetricAbc, d -> BigDecimal.valueOf(8), FAKE_METRICS_END);
        fakeMetrics.recordAllUntil(fixedMetricXyz, d -> BigDecimal.valueOf(15.3), FAKE_METRICS_END);
        fakeMetrics.recordAllUntil(growingMetricAbc, c -> {
                    var duration = c.durationSinceStart();
                    return BigDecimal.valueOf(duration.toMinutes()).divide(BigDecimal.valueOf(10));
                },
                FAKE_METRICS_END);
        fakeMetrics.recordAllUntil(growingMetricXyz, c -> {
            var d = c.durationSinceStart();
            return BigDecimal.valueOf(d.toMinutes())
                    .divide(BigDecimal.valueOf(10)).subtract(
                            BigDecimal.valueOf(d.toHoursPart()).multiply(BigDecimal.valueOf(d.toDaysPart()))
                    ).max(BigDecimal.ZERO);
        }, FAKE_METRICS_END);

        fakeMetrics.injectInto(PROMETHEUS);
    }

    @Test
    void queryValueAt() {
        var api = PrometheusApiConfig.builder()
                .url(PROMETHEUS.getApiUrl())
                .headers(Map.of())
                .build();
        var config = MetricCollectionConfig.builder()
                .type("prometheus")
                .resourceType("test")
                .metric("test")
                .query("fixed_metric_1")
                .interval(Duration.ofHours(1))
                .resourceIdLabel("resource")
                .build();
        var source = new PrometheusEventMetricsSource(WebClient.builder(), objectMapper, api, "prometheus-test",
                config.type());

        assertThat(source.collectMetrics(config, FAKE_METRICS_START)).satisfiesExactlyInAnyOrder(
                resourceAbc -> {
                    assertThat(resourceAbc.resourceId()).isEqualTo("abc");
                    assertThat(resourceAbc.timeInterval().getStartTime()).isEqualTo(FAKE_METRICS_START);
                    assertThat(resourceAbc.timeInterval().getEndTime()).isEqualTo(
                            FAKE_METRICS_START.plus(1, ChronoUnit.HOURS));
                    assertThat(resourceAbc.value()).isEqualTo(BigDecimal.valueOf(8));
                },
                resourceXyz -> {
                    assertThat(resourceXyz.resourceId()).isEqualTo("xyz");
                    assertThat(resourceXyz.timeInterval().getStartTime()).isEqualTo(FAKE_METRICS_START);
                    assertThat(resourceXyz.timeInterval().getEndTime()).isEqualTo(
                            FAKE_METRICS_START.plus(1, ChronoUnit.HOURS));
                    assertThat(resourceXyz.value()).isEqualTo(BigDecimal.valueOf(15.3));
                }
        );

        var dynamicConfig = MetricCollectionConfig.builder()
                .type("prometheus")
                .resourceType("test")
                .metric("test")
                .query("increase(growing_metric_1[1h])")
                .interval(Duration.ofHours(1))
                .resourceIdLabel("resource")
                .build();

        assertThat(source.collectMetrics(dynamicConfig, FAKE_METRICS_START))
                .filteredOn(m -> Objects.equals(m.resourceId(), "abc"))
                .singleElement()
                .satisfies(metric -> {
                    assertThat(metric.timeInterval().getStartTime()).isEqualTo(FAKE_METRICS_START);
                    assertThat(metric.timeInterval().getDuration()).isEqualTo(Duration.ofHours(1));
                    // value is <minutes>/10 -> increases with 6 per hour
                    assertThat(metric.value()).isEqualTo(BigDecimal.valueOf(6));
                });
    }

}