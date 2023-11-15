package com.contentgrid.surveyor.infrastructure.collector.pegman;

import com.contentgrid.surveyor.infrastructure.collector.pegman.transport.PegmanMetric;
import com.contentgrid.surveyor.infrastructure.collector.pegman.transport.PegmanMetric.MetricMeasuredValue;
import com.contentgrid.surveyor.infrastructure.collector.pegman.transport.PegmanMetricAssembler;
import com.contentgrid.surveyor.jackson.streaming.parser.JsonStreamParser;
import com.contentgrid.surveyor.spi.ResourceDefinition;
import com.contentgrid.surveyor.spi.TimeInterval;
import com.contentgrid.surveyor.spi.collector.CollectedMeasurement;
import com.contentgrid.surveyor.spi.collector.MeasurementCollector;
import com.contentgrid.surveyor.spi.config.MetricCollectionConfig;
import com.contentgrid.surveyor.spi.MetricCollectorSystemType;
import com.contentgrid.surveyor.values.ResourceId;
import com.contentgrid.surveyor.values.SourceName;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.config.HypermediaWebClientConfigurer;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.Builder;
import reactor.core.publisher.Flux;

@Slf4j
@RequiredArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
public class PegmanMeasurementCollector implements MeasurementCollector {

    @NonNull
    private final WebClient webClient;

    @NonNull
    private final ObjectMapper objectMapper;

    @ToString.Include
    @NonNull
    private final SourceName systemName;

    @ToString.Include
    @NonNull
    private final MetricCollectorSystemType type;

    public PegmanMeasurementCollector(
            WebClient.Builder clientBuilder,
            PegmanApiConfig config,
            HypermediaWebClientConfigurer webClientConfigurer,
            ObjectMapper objectMapper,
            SourceName systemName,
            MetricCollectorSystemType type
    ) {
        this(
                configureClient(clientBuilder, config.andThen(webClientConfigurer::registerHypermediaTypes)),
                objectMapper,
                systemName,
                type
        );
    }

    private static WebClient configureClient(WebClient.Builder clientBuilder, Consumer<Builder> configurer) {
        var builder = clientBuilder.clone();
        configurer.accept(builder);
        return builder.build();
    }


    @Override
    public MetricCollectorSystemType getSystemType() {
        return type;
    }

    @Override
    public Optional<ResourceDefinition> resourceDefinition(MetricCollectionConfig config) {
        if (Objects.equals(config.type(), type)) {
            return Optional.of(new ResourceDefinition(systemName, config.resourceType(), config.metric()));
        }
        return Optional.empty();
    }

    @Override
    public Publisher<CollectedMeasurement> collectMeasurements(MetricCollectionConfig config, Instant startedAt) {
        var interval = TimeInterval.after(startedAt, config.interval());
        return collectMeasurementsForBackfilling(config, interval);
    }

    @Override
    public Publisher<CollectedMeasurement> collectMeasurementsForBackfilling(MetricCollectionConfig config,
            TimeInterval interval) {
        var recalculatedInterval = interval.alignedToMultipleOf(config.interval());

        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/metrics/{query}")
                        .queryParam("start", "{start}")
                        .queryParam("end", "{end}")
                        .build(Map.of(
                                "query", config.query(),
                                "start", recalculatedInterval.getStartTime().toString(),
                                "end", recalculatedInterval.getEndTime().toString()
                        ))
                )
                .exchangeToFlux(response -> {
                    if (response.statusCode().is2xxSuccessful()) {
                        return response.body((body, context) -> JsonStreamParser.parse(body.getBody(),
                                new PegmanMetricAssembler(objectMapper), objectMapper));
                    } else {
                        return Flux.from(response.createError());
                    }
                })
                .flatMap(response -> this.toMetrics(config, response))
                .filter(metric -> recalculatedInterval.contains(metric.timeInterval()).isContained());
    }

    private Flux<CollectedMeasurement> toMetrics(MetricCollectionConfig config,
            CollectionModel<PegmanMetric> response) {
        return Flux.fromIterable(response)
                .flatMap(metric -> this.toMetrics(config, metric));
    }

    private Flux<CollectedMeasurement> toMetrics(MetricCollectionConfig config, PegmanMetric metric) {
        return Flux.fromIterable(metric.getData())
                .map(value -> this.createMetric(config, metric, value));
    }

    private CollectedMeasurement createMetric(MetricCollectionConfig config, PegmanMetric metric,
            MetricMeasuredValue value) {
        return new CollectedMeasurement(
                new ResourceDefinition(this.systemName, config.resourceType(), config.metric()),
                ResourceId.of(metric.getResource().resourceId()),
                Map.of(),
                TimeInterval.between(value.startTime(), value.endTime()),
                value.value()
        );
    }
}
