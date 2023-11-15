package com.contentgrid.surveyor.usecase.metrics;

import com.contentgrid.surveyor.api.metrics.FindBillingMetrics;
import com.contentgrid.surveyor.api.metrics.FindExportedMetrics;
import com.contentgrid.surveyor.api.metrics.ExportedMetrics;
import com.contentgrid.surveyor.api.metrics.FindInsightMetrics;
import com.contentgrid.surveyor.api.metrics.Metric;
import com.contentgrid.surveyor.api.metrics.Resource;
import com.contentgrid.surveyor.api.metrics.ResourceMetric;
import com.contentgrid.surveyor.spi.TimeInterval;
import com.contentgrid.surveyor.spi.config.FindMeasurementAggregationConfigurationSpiPort;
import com.contentgrid.surveyor.spi.config.FindResourceDefinitionsSpiPort;
import com.contentgrid.surveyor.spi.storage.AggregateMeasurementsSpiPort;
import com.contentgrid.surveyor.spi.storage.Measurement;
import com.contentgrid.surveyor.spi.storage.aggregation.AggregationConfiguration;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class FindMetricsUseCase implements FindInsightMetrics, FindBillingMetrics, FindExportedMetrics {

    private final FindMeasurementAggregationConfigurationSpiPort findMeasurementAggregationConfigurationSpiPort;
    private final AggregateMeasurementsSpiPort eventCountMetricSpiPort;
    private final FindResourceDefinitionsSpiPort findResourceDefinitionsSpiPort;

    @Override
    public Publisher<ExportedMetrics> findMetricsForExport(ExportMetricsCommand command) {
        var definitions = findResourceDefinitionsSpiPort.findResourceDefinitions(command.resourceType(),
                command.metric());
        if (definitions.isEmpty()) {
            return Flux.empty();
        }

        var interval = TimeInterval.between(command.start(), command.end());

        return Flux.fromIterable(definitions)
                .flatMap(definition -> eventCountMetricSpiPort.findMeasurements(definition, interval,
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
        var metrics = findResourceDefinitionsSpiPort.findResourceDefinitions(command.system(),
                        command.resourceType()).stream()
                .map(definition -> definition.createMetric(command.resourceId(), Map.of()));

        var interval = getInterval(command.start(), command.end());
        var step = Optional.ofNullable(command.step())
                .orElseGet(() -> interval.getDuration().toDays() > 2 ? Duration.ofDays(1) : Duration.ofHours(1));

        return Flux.fromStream(metrics)
                .map(metric -> new FluxExportedMetrics(
                        this.toResource(metric),
                        Flux.defer(() -> eventCountMetricSpiPort.findMeasurements(
                                metric,
                                interval,
                                findMeasurementAggregationConfigurationSpiPort.getInsightsAggregationConfiguration(
                                        metric.getResourceDefinition(), step)
                        )).map(this::toMetric)
                ));
    }

    @Override
    public Publisher<ResourceMetric> findMetricsForBilling(BillingMetricsCommand command) {
        var metrics = findResourceDefinitionsSpiPort.findResourceDefinitions(command.system(),
                        command.resourceType()).stream()
                .map(definition -> definition.createMetric(command.resourceId(), Map.of()));

        var interval = getInterval(command.start(), command.end());

        return Flux.fromStream(metrics)
                .flatMap(metric -> {
                    var groupingConfig = findMeasurementAggregationConfigurationSpiPort.getBillingAggregationConfiguration(
                            metric.getResourceDefinition(), interval.getDuration());

                    return eventCountMetricSpiPort.getAggregatedMeasurements(metric, interval, groupingConfig)
                            .map(measurement -> new ResourceMetricImpl(this.toResource(measurement),
                                    this.toMetric(measurement)));
                });

    }

    private Metric toMetric(Measurement metric) {
        return new Metric(
                metric.getMeasureInterval().getStartTime(),
                metric.getMeasureInterval().getEndTime(),
                metric.getValue()
        );
    }

    private Resource toResource(Measurement measurement) {
        return toResource(measurement.getMetric());
    }

    private Resource toResource(com.contentgrid.surveyor.spi.resources.Metric metric) {
        var identity = metric.getResourceIdentity();
        return new Resource(
                identity.getSourceSystem(),
                identity.getResourceType(),
                metric.getMetricName(),
                identity.getResourceId()
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
