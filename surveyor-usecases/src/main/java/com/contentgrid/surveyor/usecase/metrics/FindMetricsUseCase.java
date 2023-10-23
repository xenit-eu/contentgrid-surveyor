package com.contentgrid.surveyor.usecase.metrics;

import com.contentgrid.surveyor.api.metrics.BillingMetrics;
import com.contentgrid.surveyor.api.metrics.ExportMetrics;
import com.contentgrid.surveyor.api.metrics.ExportedMetrics;
import com.contentgrid.surveyor.api.metrics.FindInsightMetrics;
import com.contentgrid.surveyor.api.metrics.Metric;
import com.contentgrid.surveyor.api.metrics.Resource;
import com.contentgrid.surveyor.api.metrics.ResourceMetric;
import com.contentgrid.surveyor.spi.TimeInterval;
import com.contentgrid.surveyor.spi.config.FindResourceAggregationConfigurationSpiPort;
import com.contentgrid.surveyor.spi.config.FindResourceDefinitionsSpiPort;
import com.contentgrid.surveyor.spi.storage.AggregateEventCountMetricSpiPort;
import com.contentgrid.surveyor.spi.storage.EventCountMetric;
import com.contentgrid.surveyor.spi.storage.aggregation.AggregationConfiguration;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class FindMetricsUseCase implements FindInsightMetrics, BillingMetrics, ExportMetrics {

    private final FindResourceAggregationConfigurationSpiPort findResourceAggregationConfigurationSpiPort;
    private final AggregateEventCountMetricSpiPort eventCountMetricSpiPort;
    private final FindResourceDefinitionsSpiPort findResourceDefinitionsSpiPort;

    @Override
    public Publisher<ExportedMetrics> findMetricsForExport(ExportMetricsCommand command) {
        var maybeDefinition = findResourceDefinitionsSpiPort.findResourceDefinitions(command.resourceType())
                .stream()
                .filter(def -> Objects.equals(def.metricName(), command.metric()))
                .findAny();
        if (maybeDefinition.isEmpty()) {
            return Flux.empty();
        }

        var interval = TimeInterval.between(command.start(), command.end());

        return Flux.from(eventCountMetricSpiPort.findEventCountMetrics(maybeDefinition.orElseThrow(), interval,
                        AggregationConfiguration.builder().finallyDontAggregate()))
                // Buffering until changed means that there could be multiple sets for the same resource
                // This should be fine as the response does not assume that there is only a single set for a single resource
                .bufferUntilChanged(this::toResource)
                .map(metrics -> metrics.stream().collect(Collectors.groupingBy(
                        this::toResource,
                        Collectors.mapping(this::toMetric, Collectors.toList()))
                ))
                .flatMap(groupedMetrics -> Flux.fromStream(groupedMetrics.entrySet().stream()
                        .map(entry -> new StaticExportedMetrics(entry.getKey(), entry.getValue()))
                ));
    }

    @Override
    public Publisher<ExportedMetrics> findMetricsForInsights(FindInsightMetricsCommand command) {
        var resources = findResourceDefinitionsSpiPort.findResourceDefinitions(command.system(),
                        command.resourceType()).stream()
                .map(definition -> new com.contentgrid.surveyor.spi.storage.Resource(definition, command.resourceId()));

        var interval = getInterval(command.start(), command.end());
        var step = Optional.ofNullable(command.step())
                .orElseGet(() -> interval.getDuration().toDays() > 2 ? Duration.ofDays(1) : Duration.ofHours(1));

        return Flux.fromStream(resources)
                .map(resource -> new FluxExportedMetrics(
                        this.toResource(resource),
                        Flux.defer(() -> eventCountMetricSpiPort.findEventCountMetrics(
                                resource,
                                interval,
                                findResourceAggregationConfigurationSpiPort.getInsightsAggregationConfiguration(
                                        resource.getDefinition(), step)
                        )).map(this::toMetric)
                ));
    }

    @Override
    public Publisher<ResourceMetric> findMetricsForBilling(BillingMetricsCommand command) {
        var resources = findResourceDefinitionsSpiPort.findResourceDefinitions(command.system(),
                        command.resourceType()).stream()
                .map(definition -> new com.contentgrid.surveyor.spi.storage.Resource(definition, command.resourceId()));

        var interval = getInterval(command.start(), command.end());

        return Flux.fromStream(resources)
                .flatMap(resource -> {
                    var groupingConfig = findResourceAggregationConfigurationSpiPort.getBillingAggregationConfiguration(
                            resource.getDefinition(), interval.getDuration());

                    return Mono.from(
                                    eventCountMetricSpiPort.getAggregatedEventCountMetric(resource, interval, groupingConfig))
                            .map(metric -> new ResourceMetricImpl(this.toResource(resource), this.toMetric(metric)));
                });

    }

    private Metric toMetric(EventCountMetric metric) {
        return new Metric(
                metric.getMeasureInterval().getStartTime(),
                metric.getMeasureInterval().getEndTime(),
                metric.getValue()
        );
    }

    private Resource toResource(EventCountMetric metric) {
        return toResource(metric.getResource());
    }

    private Resource toResource(com.contentgrid.surveyor.spi.storage.Resource resource) {
        var definition = resource.getDefinition();
        return new Resource(
                definition.sourceSystem(),
                definition.resourceType(),
                resource.getResourceId(),
                definition.metricName()
        );
    }

    private TimeInterval getInterval(Instant start, Instant end) {
        if (start == null) {
            start = Instant.now().truncatedTo(ChronoUnit.DAYS);
        }
        if (end == null) {
            end = start.plus(1, ChronoUnit.DAYS);
        }
        return TimeInterval.between(start, end).alignedToMultipleOf(Duration.ofHours(1));
    }

    @RequiredArgsConstructor
    private static class StaticExportedMetrics implements ExportedMetrics {

        private final Resource resource;
        private final List<Metric> metrics;

        @Override
        public Resource resource() {
            return resource;
        }

        @Override
        public Publisher<Metric> metrics() {
            return Flux.fromIterable(metrics);
        }
    }

    private record FluxExportedMetrics(
            Resource resource,
            Publisher<Metric> metrics
    ) implements ExportedMetrics {


    }

    private record ResourceMetricImpl(
            Resource resource,
            Metric metric
    ) implements com.contentgrid.surveyor.api.metrics.ResourceMetric {

    }
}
