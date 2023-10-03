package com.contentgrid.surveyor.infrastructure.source.prometheus;

import com.contentgrid.surveyor.infrastructure.source.prometheus.transport.PrometheusResponse;
import com.contentgrid.surveyor.infrastructure.source.prometheus.transport.PrometheusResponse.PrometheusMatrixData;
import com.contentgrid.surveyor.infrastructure.source.prometheus.transport.PrometheusResponse.PrometheusVectorData;
import com.contentgrid.surveyor.infrastructure.source.prometheus.transport.PrometheusResponse.Status;
import com.contentgrid.surveyor.infrastructure.source.prometheus.transport.PrometheusVectorResult;
import com.contentgrid.surveyor.spi.source.CollectedMetric;
import com.contentgrid.surveyor.spi.source.EventMetricsSource;
import com.contentgrid.surveyor.spi.source.TimeInterval;
import java.time.Instant;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.Builder;
import reactor.core.publisher.Flux;

@Slf4j
@RequiredArgsConstructor
public class PrometheusEventMetricsSource implements EventMetricsSource {

    private final WebClient webClient;
    private final PrometheusMetricCollectionConfig config;

    public PrometheusEventMetricsSource(WebClient.Builder clientBuilder, PrometheusMetricCollectionConfig config) {
        this(configureClient(clientBuilder, config.api()), config);
    }

    private static WebClient configureClient(WebClient.Builder clientBuilder, Consumer<Builder> configurer) {
        var builder = clientBuilder.clone();
        configurer.accept(builder);
        return builder.build();
    }

    @Override
    public Stream<CollectedMetric> collectMetrics(Instant referenceTime) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path("api/v1/query")
                        .queryParam("query", config.query())
                        .queryParam("time", referenceTime.toString())
                        .build()
                )
                .retrieve()
                .bodyToMono(PrometheusResponse.class)
                .flatMapMany(this::toMetrics)
                .toStream();
    }

    @Override
    public Stream<CollectedMetric> collectMetricsForBackfilling(TimeInterval interval)
            throws CollectionFailedException {
        var recalculatedInterval = interval.alignedToMultipleOf(config.snapshotInterval());

        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path("api/v1/query_range")
                        .queryParam("query", config.query())
                        .queryParam("start", recalculatedInterval.getStartTime().toString())
                        .queryParam("end", recalculatedInterval.getEndTime().toString())
                        .queryParam("step", config.snapshotInterval().getSeconds())
                        .build()
                )
                .retrieve()
                .bodyToMono(PrometheusResponse.class)
                .flatMapMany(this::toMetrics)
                .filter(metric -> metric.getTimeInterval().contains(recalculatedInterval).isContained())
                .toStream();
    }

    private Flux<CollectedMetric> toMetrics(PrometheusResponse response) {
        if (response.status() == Status.ERROR) {
            return Flux.error(new CollectionFailedException(
                    "Failed to query '%s': %s".formatted(config.query(), response.error())));
        }
        if (response.warnings() != null && !response.warnings().isEmpty()) {
            log.warn("Warnings for query '{}': {}", config.query(), response.warnings());
        }
        if (response.data() instanceof PrometheusVectorData vector) {
            return Flux.fromIterable(vector.getResult())
                    .map(this::createMetric);
        } else if (response.data() instanceof PrometheusMatrixData matrix) {
            return Flux.fromIterable(matrix.getResult())
                    .flatMap(result -> Flux.fromStream(result.asVectors()))
                    .map(this::createMetric);
        }
        return Flux.error(new CollectionFailedException("Response data is not instant vector"));
    }

    private CollectedMetric createMetric(PrometheusVectorResult prometheusVectorResult) {
        return new CollectedMetric(
                config.resourceType(),
                Objects.requireNonNull(prometheusVectorResult.metric().get(config.resourceIdLabel()),
                        () -> "Metric is missing label %s".formatted(config.resourceIdLabel())),
                config.metric(),
                TimeInterval.after(prometheusVectorResult.value().timestamp(), config.snapshotInterval()),
                prometheusVectorResult.value().value()
        );
    }
}
