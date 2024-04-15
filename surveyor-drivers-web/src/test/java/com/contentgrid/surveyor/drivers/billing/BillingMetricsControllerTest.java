package com.contentgrid.surveyor.drivers.billing;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.contentgrid.surveyor.api.metrics.AggregateBillingMetrics;
import com.contentgrid.surveyor.spi.TimeInterval;
import com.contentgrid.surveyor.spi.resources.LinkedMeasurements;
import com.contentgrid.surveyor.spi.resources.Metric;
import com.contentgrid.surveyor.spi.resources.ResourceIdentity;
import com.contentgrid.surveyor.spi.resources.ResourceLinkage;
import com.contentgrid.surveyor.spi.storage.Measurement;
import com.contentgrid.surveyor.values.MetricName;
import com.contentgrid.surveyor.values.ResourceId;
import com.contentgrid.surveyor.values.ResourceType;
import com.contentgrid.surveyor.values.SourceName;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;

@ContextConfiguration(classes = {AggregateBillingMetrics.class, BillingMetricsController.class, SurveyorBillingConfiguration.class})
@ExtendWith(SpringExtension.class)
@WebFluxTest(BillingMetricsController.class)
@AutoConfigureWebTestClient
class BillingMetricsControllerTest {

    @MockBean
    AggregateBillingMetrics aggregateBillingMetrics;

    @Autowired
    WebTestClient webTestClient;


    @Test
    void test() throws Exception {
        given(aggregateBillingMetrics.findMetricsForBilling(any()))
                .willReturn(Flux.just(data()));

        webTestClient.get().uri("/metrics/billing.csv?year=2024&month=1").exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.parseMediaType("text/csv"))
                .expectBody().consumeWith((entityExchangeResult -> {
                    assertArrayEquals("""
                            /orgs/123,/projects/123,/applications/123,1000000000,50000,52428800000,100000
                            /orgs/456,/projects/456,/applications/456,50000,200,26214400,400
                            """.getBytes(StandardCharsets.UTF_8),
                            entityExchangeResult.getResponseBody());
                }));
    }

    // @formatter:off
    static LinkedMeasurements[] data() {
        return new LinkedMeasurements[] {
            new LinkedMeasurements(
                Map.of(
                    MetricName.of("objects_count"), new Measurement(
                        TimeInterval.between(
                            Instant.parse("2024-04-01T00:00:00.00Z"),
                            Instant.parse("2024-04-30T23:59:59.99Z")
                        ), new Metric(
                            new ResourceIdentity(SourceName.of("s"), ResourceType.of("storage"), ResourceId.of("123")),
                            MetricName.of("objects_count"),
                            Map.of()
                        ),
                        BigDecimal.valueOf(50000)
                    ),
                    MetricName.of("stored_bytes"), new Measurement(
                        TimeInterval.between(
                            Instant.parse("2024-04-01T00:00:00.00Z"),
                            Instant.parse("2024-04-30T23:59:59.99Z")
                        ), new Metric(
                            new ResourceIdentity(SourceName.of("s"), ResourceType.of("storage"), ResourceId.of("123")),
                            MetricName.of("stored_bytes"),
                            Map.of()
                        ),
                        BigDecimal.valueOf(52428800000L)
                    ),
                    MetricName.of("request_count"), new Measurement(
                        TimeInterval.between(
                            Instant.parse("2024-04-01T00:00:00.00Z"),
                            Instant.parse("2024-04-30T23:59:59.99Z")
                        ), new Metric(
                            new ResourceIdentity(SourceName.of("s"), ResourceType.of("api"), ResourceId.of("api-123")),
                            MetricName.of("request_count"),
                            Map.of()
                        ),
                        BigDecimal.valueOf(1000000000L)
                    ),
                    MetricName.of("estimated_count"), new Measurement(
                        TimeInterval.between(
                            Instant.parse("2024-04-01T00:00:00.00Z"),
                            Instant.parse("2024-04-30T23:59:59.99Z")
                        ), new Metric(
                            new ResourceIdentity(SourceName.of("s"), ResourceType.of("api"), ResourceId.of("db-123")),
                            MetricName.of("estimated_count"),
                            Map.of()
                        ),
                        BigDecimal.valueOf(100000L)
                    )
                ),
                new ResourceLinkage("/applications/123", "/projects/123", "/orgs/123")
            ),
            new LinkedMeasurements(
                Map.of(
                    MetricName.of("objects_count"), new Measurement(
                        TimeInterval.between(
                            Instant.parse("2024-04-01T00:00:00.00Z"),
                            Instant.parse("2024-04-30T23:59:59.99Z")
                        ), new Metric(
                            new ResourceIdentity(SourceName.of("s"), ResourceType.of("storage"), ResourceId.of("456")),
                            MetricName.of("objects_count"),
                            Map.of()
                        ),
                        BigDecimal.valueOf(200)
                    ),
                    MetricName.of("stored_bytes"), new Measurement(
                        TimeInterval.between(
                            Instant.parse("2024-04-01T00:00:00.00Z"),
                            Instant.parse("2024-04-30T23:59:59.99Z")
                        ), new Metric(
                            new ResourceIdentity(SourceName.of("s"), ResourceType.of("storage"), ResourceId.of("456")),
                            MetricName.of("stored_bytes"),
                            Map.of()
                        ),
                        BigDecimal.valueOf(26214400L)
                    ),
                    MetricName.of("request_count"), new Measurement(
                        TimeInterval.between(
                            Instant.parse("2024-04-01T00:00:00.00Z"),
                            Instant.parse("2024-04-30T23:59:59.99Z")
                        ), new Metric(
                            new ResourceIdentity(SourceName.of("s"), ResourceType.of("api"), ResourceId.of("api-456")),
                            MetricName.of("request_count"),
                            Map.of()
                        ),
                        BigDecimal.valueOf(50000L)
                    ),
                    MetricName.of("estimated_count"), new Measurement(
                        TimeInterval.between(
                            Instant.parse("2024-04-01T00:00:00.00Z"),
                            Instant.parse("2024-04-30T23:59:59.99Z")
                        ), new Metric(
                            new ResourceIdentity(SourceName.of("s"), ResourceType.of("api"), ResourceId.of("db-456")),
                            MetricName.of("estimated_count"),
                            Map.of()
                        ),
                        BigDecimal.valueOf(400L)
                    )
                ),
                new ResourceLinkage("/applications/456", "/projects/456", "/orgs/456")
            )
        };
    }
    // @formatter:on
}