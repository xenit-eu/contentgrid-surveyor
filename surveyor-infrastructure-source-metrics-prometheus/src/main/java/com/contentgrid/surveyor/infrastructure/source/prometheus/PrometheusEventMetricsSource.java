package com.contentgrid.surveyor.infrastructure.source.prometheus;

import com.contentgrid.surveyor.infrastructure.source.prometheus.transport.PrometheusResponse;
import com.contentgrid.surveyor.infrastructure.source.prometheus.transport.PrometheusResponse.PrometheusMatrixData;
import com.contentgrid.surveyor.infrastructure.source.prometheus.transport.PrometheusResponse.PrometheusVectorData;
import com.contentgrid.surveyor.infrastructure.source.prometheus.transport.PrometheusResponse.Status;
import com.contentgrid.surveyor.infrastructure.source.prometheus.transport.PrometheusVectorResult;
import com.contentgrid.surveyor.spi.TimeInterval;
import com.contentgrid.surveyor.spi.source.CollectedMetric;
import com.contentgrid.surveyor.spi.source.EventMetricsSource;
import com.contentgrid.surveyor.spi.source.MetricCollectionConfig;
import java.time.Instant;
import java.util.Map;
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
    private final PrometheusApiConfig config;

    public PrometheusEventMetricsSource(WebClient.Builder clientBuilder, PrometheusApiConfig config) {
        this(configureClient(clientBuilder, config), config);
    }

    private static WebClient configureClient(WebClient.Builder clientBuilder, Consumer<Builder> configurer) {
        var builder = clientBuilder.clone();
        configurer.accept(builder);
        return builder.build();
    }

    @Override
    public boolean supports(MetricCollectionConfig config) {
        return Objects.equals(config.resourceDefinition().sourceSystem(), this.config.system());
    }

    @Override
    public Stream<CollectedMetric> collectMetrics(MetricCollectionConfig config, Instant referenceTime) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/v1/query")
                        .queryParam("query", "{query}")
                        .queryParam("time", "{time}")
                        .build(Map.of(
                                "query", config.query(),
                                "time", referenceTime.toString()
                        ))
                )
                .retrieve()
                .bodyToMono(PrometheusResponse.class)
                .flatMapMany(response -> this.toMetrics(config, response))
                .toStream();
    }

    @Override
    public Stream<CollectedMetric> collectMetricsForBackfilling(MetricCollectionConfig config, TimeInterval interval)
            throws CollectionFailedException {
        var recalculatedInterval = interval.alignedToMultipleOf(config.interval());

        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/v1/query_range")
                        .queryParam("query", "{q}")
                        .queryParam("start", "{start}")
                        .queryParam("end", "{end}")
                        .queryParam("step", "{step}")
                        .build(Map.of(
                                "q", config.query(),
                                "start", recalculatedInterval.getStartTime().toString(),
                                "end", recalculatedInterval.getEndTime().toString(),
                                "step", config.interval().getSeconds()
                        ))
                )
                .retrieve()
                .bodyToMono(PrometheusResponse.class)
                .flatMapMany(response -> this.toMetrics(config, response))
                .filter(metric -> recalculatedInterval.contains(metric.timeInterval()).isContained())
                .toStream();
    }

    private Flux<CollectedMetric> toMetrics(MetricCollectionConfig config, PrometheusResponse response) {
        if (response.status() == Status.ERROR) {
            return Flux.error(new CollectionFailedException(
                    "Failed to query '%s': %s".formatted(config.query(), response.error())));
        }
        if (response.warnings() != null && !response.warnings().isEmpty()) {
            log.warn("Warnings for query '{}': {}", config.query(), response.warnings());
        }
        if (response.data() instanceof PrometheusVectorData vector) {
            return Flux.fromIterable(vector.getResult())
                    .map(result -> this.createMetric(config, result));
        } else if (response.data() instanceof PrometheusMatrixData matrix) {
            return Flux.fromIterable(matrix.getResult())
                    .flatMap(result -> Flux.fromStream(result.asVectors()))
                    .map(result -> this.createMetric(config, result));
        }
        return Flux.error(new CollectionFailedException("Response data is not instant vector"));
    }

    private CollectedMetric createMetric(MetricCollectionConfig config, PrometheusVectorResult prometheusVectorResult) {
        return new CollectedMetric(
                config.resourceDefinition(),
                Objects.requireNonNull(prometheusVectorResult.metric().get(config.resourceIdLabel()),
                        () -> "Metric is missing label %s".formatted(config.resourceIdLabel())),
                TimeInterval.after(prometheusVectorResult.value().timestamp(), config.interval()),
                prometheusVectorResult.value().value()
        );
    }
}
