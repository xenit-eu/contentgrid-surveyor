package com.contentgrid.surveyor.infrastructure.source.prometheus;

import com.contentgrid.surveyor.infrastructure.source.prometheus.transport.PrometheusMatrixResult;
import com.contentgrid.surveyor.infrastructure.source.prometheus.transport.PrometheusResult;
import com.contentgrid.surveyor.infrastructure.source.prometheus.transport.PrometheusResultAssembler;
import com.contentgrid.surveyor.infrastructure.source.prometheus.transport.PrometheusVectorResult;
import com.contentgrid.surveyor.jackson.streaming.parser.JsonStreamParser;
import com.contentgrid.surveyor.spi.ResourceDefinition;
import com.contentgrid.surveyor.spi.TimeInterval;
import com.contentgrid.surveyor.spi.source.CollectedMetric;
import com.contentgrid.surveyor.spi.source.EventMetricsSource;
import com.contentgrid.surveyor.spi.config.MeasurementCollectionConfig;
import com.contentgrid.surveyor.spi.MetricSourceSystemType;
import com.contentgrid.surveyor.values.ResourceId;
import com.contentgrid.surveyor.values.SourceName;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.Builder;
import reactor.core.publisher.Flux;

@Slf4j
@RequiredArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
public class PrometheusEventMetricsSource implements EventMetricsSource {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    @ToString.Include
    private final SourceName systemName;
    @ToString.Include
    private final MetricSourceSystemType type;

    public PrometheusEventMetricsSource(WebClient.Builder clientBuilder, ObjectMapper objectMapper,
            PrometheusApiConfig config, SourceName systemName,
            MetricSourceSystemType type) {
        this(configureClient(clientBuilder, config), objectMapper, systemName, type);
    }

    private static WebClient configureClient(WebClient.Builder clientBuilder, Consumer<Builder> configurer) {
        var builder = clientBuilder.clone();
        configurer.accept(builder);
        return builder.build();
    }


    @Override
    public MetricSourceSystemType getSystemType() {
        return type;
    }

    @Override
    public Optional<ResourceDefinition> resourceDefinition(MeasurementCollectionConfig config) {
        if (Objects.equals(config.type(), type)) {
            return Optional.of(new ResourceDefinition(systemName, config.resourceType(), config.metric()));
        }
        return Optional.empty();
    }

    @Override
    public Publisher<CollectedMetric> collectMetrics(MeasurementCollectionConfig config, Instant startedAt) {
        // Prometheus range queries are covering the period *before* the time they are queried
        // So, a range query [1h] at 12:00 covers data from 11:00 -> 12:00
        var endTime = startedAt.plus(config.interval());
        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/v1/query")
                        .queryParam("query", "{query}")
                        .queryParam("time", "{time}")
                        .build(Map.of(
                                "query", config.query(),
                                "time", endTime.toString()
                        ))
                )
                .exchangeToFlux(response -> {
                    if (response.statusCode().isError()) {
                        return response.createException().flatMapMany(Flux::error);
                    }
                    return response.body((httpResponse, context) -> JsonStreamParser.parse(httpResponse.getBody(),
                            new PrometheusResultAssembler<>(objectMapper, PrometheusVectorResult.class),
                            objectMapper));
                })
                .flatMap(assembly -> this.handleAssembly(config, assembly));
    }

    @Override
    public Publisher<CollectedMetric> collectMetricsForBackfilling(MeasurementCollectionConfig config,
            TimeInterval interval) {
        var recalculatedInterval = interval.alignedToMultipleOf(config.interval());
        // Prometheus range queries are covering the period *before* the time they are queried
        // So, a range query [1h] at 12:00 covers data from 11:00 -> 12:00
        // The query interval needs to be adjusted, so when the requested interval is [11:00, 12:00),
        // the data we need to query from prometheus is [12:00, 13:00) (shifted by one config.interval())
        var queryInterval = recalculatedInterval.shiftedBy(config.interval());

        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/v1/query_range")
                        .queryParam("query", "{q}")
                        .queryParam("start", "{start}")
                        .queryParam("end", "{end}")
                        .queryParam("step", "{step}")
                        .build(Map.of(
                                "q", config.query(),
                                "start", queryInterval.getStartTime().toString(),
                                "end", queryInterval.getEndTime().toString(),
                                "step", config.interval().getSeconds()
                        ))
                )
                .exchangeToFlux(response -> {
                    if (response.statusCode().isError()) {
                        return response.createException().flatMapMany(Flux::error);
                    }
                    return response.body((httpResponse, context) -> JsonStreamParser.parse(httpResponse.getBody(),
                            new PrometheusResultAssembler<>(objectMapper, PrometheusMatrixResult.class),
                            objectMapper));
                })
                .flatMap(assembly -> this.handleAssembly(config, assembly))
                .filter(metric -> recalculatedInterval.contains(metric.timeInterval()).isContained());
    }

    private Flux<CollectedMetric> handleAssembly(MeasurementCollectionConfig config,
            PrometheusResultAssembler.AssemblyResult<?> result) {
        if (result instanceof PrometheusResultAssembler<?>.ErrorAssemblyResult error) {
            return Flux.error(new CollectionFailedException(
                    "Failed to query '%s': %s".formatted(config.query(), error.getError())));
        } else if (result instanceof PrometheusResultAssembler<?>.WarningsAssemblyResult warnings) {
            log.warn("Warnings for query '{}': {}", config.query(), warnings.getWarnings());
            return Flux.empty();
        } else if (result instanceof PrometheusResultAssembler<?>.DataAssemblyResult data) {
            return toMetrics(config, data.getData());
        } else {
            return Flux.error(new CollectionFailedException("Unknown type of assembly result"));
        }
    }

    private Flux<CollectedMetric> toMetrics(MeasurementCollectionConfig config, PrometheusResult data) {
        if (data instanceof PrometheusVectorResult vector) {
            return Flux.just(this.createMetric(config, vector));
        } else if (data instanceof PrometheusMatrixResult matrix) {
            return Flux.fromStream(matrix.asVectors())
                    .map(result -> this.createMetric(config, result));
        }
        return Flux.error(new CollectionFailedException("Response data is not vector or matrix"));
    }

    private CollectedMetric createMetric(MeasurementCollectionConfig config,
            PrometheusVectorResult prometheusVectorResult) {
        return new CollectedMetric(
                new ResourceDefinition(this.systemName, config.resourceType(), config.metric()),
                ResourceId.of(Objects.requireNonNull(prometheusVectorResult.metric().get(config.resourceIdLabel()),
                        () -> "Metric is missing label %s".formatted(config.resourceIdLabel()))),
                Map.of(),
                TimeInterval.before(prometheusVectorResult.value().timestamp(), config.interval()),
                prometheusVectorResult.value().value()
        );
    }
}
