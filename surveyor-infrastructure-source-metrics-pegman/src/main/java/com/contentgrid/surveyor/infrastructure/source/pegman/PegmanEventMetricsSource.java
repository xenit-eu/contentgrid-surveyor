package com.contentgrid.surveyor.infrastructure.source.pegman;

import com.contentgrid.surveyor.infrastructure.source.pegman.transport.PegmanMetric;
import com.contentgrid.surveyor.infrastructure.source.pegman.transport.PegmanMetric.MetricMeasuredValue;
import com.contentgrid.surveyor.spi.ResourceDefinition;
import com.contentgrid.surveyor.spi.TimeInterval;
import com.contentgrid.surveyor.spi.source.CollectedMetric;
import com.contentgrid.surveyor.spi.source.EventMetricsSource;
import com.contentgrid.surveyor.spi.source.MetricCollectionConfig;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.config.HypermediaWebClientConfigurer;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.Builder;
import reactor.core.publisher.Flux;

@Slf4j
@RequiredArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
public class PegmanEventMetricsSource implements EventMetricsSource {

    @NonNull
    private final WebClient webClient;

    @ToString.Include
    @NonNull
    private final String systemName;

    @ToString.Include
    @NonNull
    private final String type;

    public PegmanEventMetricsSource(
            WebClient.Builder clientBuilder,
            PegmanApiConfig config,
            HypermediaWebClientConfigurer webClientConfigurer,
            String systemName,
            String type
    ) {
        this(
                configureClient(clientBuilder, config.andThen(webClientConfigurer::registerHypermediaTypes)),
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
    public Optional<ResourceDefinition> resourceDefinition(MetricCollectionConfig config) {
        if (Objects.equals(config.type(), type)) {
            return Optional.of(new ResourceDefinition(systemName, config.resourceType(), config.metric()));
        }
        return Optional.empty();
    }

    @Override
    public Stream<CollectedMetric> collectMetrics(MetricCollectionConfig config, Instant startedAt)
            throws CollectionFailedException {
        var interval = TimeInterval.after(startedAt, config.interval());
        return collectMetricsForBackfilling(config, interval);
    }

    @Override
    public Stream<CollectedMetric> collectMetricsForBackfilling(MetricCollectionConfig config, TimeInterval interval)
            throws CollectionFailedException {
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
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<CollectionModel<PegmanMetric>>() {
                })
                .flatMapMany(response -> this.toMetrics(config, response))
                .filter(metric -> recalculatedInterval.contains(metric.timeInterval()).isContained())
                .toStream();
    }

    private Flux<CollectedMetric> toMetrics(MetricCollectionConfig config, CollectionModel<PegmanMetric> response) {
        return Flux.fromIterable(response)
                .flatMap(metric -> this.toMetrics(config, metric));
    }

    private Flux<CollectedMetric> toMetrics(MetricCollectionConfig config, PegmanMetric metric) {
        return Flux.fromIterable(metric.getData())
                .map(value -> this.createMetric(config, metric, value));
    }

    private CollectedMetric createMetric(MetricCollectionConfig config, PegmanMetric metric,
            MetricMeasuredValue value) {
        return new CollectedMetric(
                new ResourceDefinition(this.systemName, config.resourceType(), config.metric()),
                metric.getResource().resourceId(),
                TimeInterval.between(value.startTime(), value.endTime()),
                value.value()
        );
    }
}
