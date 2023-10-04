package com.contentgrid.surveyor.infrastructure.source.prometheus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.contentgrid.surveyor.infrastructure.source.prometheus.transport.PrometheusResponse;
import com.contentgrid.surveyor.spi.source.EventMetricsSource.CollectionFailedException;
import com.contentgrid.surveyor.spi.source.MetricCollectionConfig;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class PrometheusEventMetricsSourceTest {

    @Container
    private static final PrometheusContainer PROMETHEUS = new PrometheusContainer()
            .withCommand("--storage.tsdb.retention.time=1y", "--config.file=/etc/prometheus/prometheus.yml");

    private static final Instant FAKE_METRICS_END = Instant.now().truncatedTo(ChronoUnit.DAYS)
            .minus(1, ChronoUnit.DAYS);
    private static final Instant FAKE_METRICS_START = FAKE_METRICS_END.minus(Duration.ofDays(80));
    private static final Duration FAKE_METRICS_INTERVAL = Duration.ofSeconds(31);

    @BeforeAll
    static void createFakeMetrics() throws IOException, InterruptedException {
        var byteArray = new ByteArrayOutputStream();
        PrintStream fakeMetrics = new PrintStream(byteArray);

        var currentTimestamp = FAKE_METRICS_START;
        while (FAKE_METRICS_END.isAfter(currentTimestamp)) {

            fakeMetrics.println(
                    "fixed_metric_1{resource=\"abc\",other=\"def\"} 8 " + currentTimestamp.getEpochSecond());
            fakeMetrics.println(
                    "fixed_metric_1{resource=\"xyz\",other=\"def\"} 15.3 " + currentTimestamp.getEpochSecond());
            fakeMetrics.println("growing_metric_1{resource=\"abc\"} " + calculateMetric(currentTimestamp,
                    d -> d.toMinutes() / 60.0d) + " " + currentTimestamp.getEpochSecond());
            fakeMetrics.println("growing_metric_1{resource=\"xyz\"} " + calculateMetric(currentTimestamp,
                    d -> Math.max(0d, d.toMinutes() / 60.0d - d.toHoursPart() * d.toDaysPart())) + " "
                    + currentTimestamp.getEpochSecond());

            currentTimestamp = currentTimestamp.plus(FAKE_METRICS_INTERVAL);
        }
        fakeMetrics.println("# EOF");

        PROMETHEUS.copyFileToContainer(Transferable.of(byteArray.toByteArray()), "/tmp/metrics");
        var result = PROMETHEUS.execInContainer("promtool", "tsdb", "create-blocks-from", "openmetrics",
                "--max-block-duration=1d", "/tmp/metrics");
        if (result.getExitCode() != 0) {
            System.out.println(result.getStdout());
            System.err.println(result.getStderr());
        }

        await()
                .atMost(1, TimeUnit.MINUTES)
                .until(() -> {
                    return WebClient.builder()
                            .baseUrl(PROMETHEUS.getApiUrl().resolve("api/v1/query").toString())
                            .build()
                            .get()
                            .uri(builder -> builder.queryParam("query", "fixed_metric_1")
                                    .queryParam("time", FAKE_METRICS_END.toString())
                                    .build()
                            )
                            .retrieve()
                            .toEntity(PrometheusResponse.class)
                            .block()
                            .getBody();
                }, body -> !body.data().getResult().isEmpty());

    }

    private static double calculateMetric(Instant currentTimestamp, Function<Duration, Double> calculation) {
        var duration = Duration.between(FAKE_METRICS_START, currentTimestamp);
        return calculation.apply(duration);
    }

    @Test
    void queryValueAt() throws CollectionFailedException {
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
        var source = new PrometheusEventMetricsSource(WebClient.builder(), api, "prometheus-test", config.type());

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
    }


}