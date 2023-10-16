package com.contentgrid.surveyor.usecase.metrics;

import com.contentgrid.surveyor.api.metrics.BillingMetrics;
import com.contentgrid.surveyor.api.metrics.ExportMetrics;
import com.contentgrid.surveyor.api.metrics.FindInsightMetrics;
import com.contentgrid.surveyor.api.metrics.Metric;
import com.contentgrid.surveyor.api.metrics.Resource;
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
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class FindMetricsUseCase implements FindInsightMetrics, BillingMetrics, ExportMetrics {

    private final FindResourceAggregationConfigurationSpiPort findResourceAggregationConfigurationSpiPort;
    private final AggregateEventCountMetricSpiPort eventCountMetricSpiPort;
    private final FindResourceDefinitionsSpiPort findResourceDefinitionsSpiPort;

    @Override
    public Map<Resource, List<Metric>> findMetricsForExport(ExportMetricsCommand command) {
        var maybeDefinition = findResourceDefinitionsSpiPort.findResourceDefinitions(command.resourceType())
                .stream()
                .filter(def -> Objects.equals(def.metricName(), command.metric()))
                .findAny();
        if (maybeDefinition.isEmpty()) {
            return Map.of();
        }

        var interval = TimeInterval.between(command.start(), command.end());

        return eventCountMetricSpiPort.findEventCountMetrics(maybeDefinition.orElseThrow(), interval,
                        AggregationConfiguration.builder().finallyDontAggregate())
                .stream()
                .collect(Collectors.groupingBy(
                        this::toResource,
                        Collectors.mapping(metric -> new Metric(
                                metric.getMeasureInterval().getStartTime(),
                                metric.getMeasureInterval().getEndTime(),
                                metric.getValue()
                        ), Collectors.toList())
                ));
    }

    @Override
    public Map<Resource, List<Metric>> findMetricsForInsights(FindInsightMetricsCommand command) {
        var definitions = findResourceDefinitionsSpiPort.findResourceDefinitions(command.system(),
                command.resourceType());

        var interval = getInterval(command.start(), command.end());
        var step = Optional.ofNullable(command.step())
                .orElseGet(() -> interval.getDuration().toDays() > 2 ? Duration.ofDays(1) : Duration.ofHours(1));

        return definitions.stream()
                .map(definition -> new com.contentgrid.surveyor.spi.storage.Resource(definition, command.resourceId()))
                .flatMap(resource -> {
                    return eventCountMetricSpiPort.findEventCountMetrics(
                            resource,
                            interval,
                            findResourceAggregationConfigurationSpiPort.getInsightsAggregationConfiguration(
                                    resource.getDefinition(), step)
                    ).stream();
                })
                .collect(
                        Collectors.groupingBy(this::toResource,
                                Collectors.mapping(metric -> new Metric(
                                        metric.getMeasureInterval().getStartTime(),
                                        metric.getMeasureInterval().getEndTime(),
                                        metric.getValue()
                                ), Collectors.toList())));
    }

    @Override
    public Map<Resource, Metric> findMetricsForBilling(BillingMetricsCommand command) {
        var definitions = findResourceDefinitionsSpiPort.findResourceDefinitions(command.system(),
                command.resourceType());

        var interval = getInterval(command.start(), command.end());

        return definitions.stream()
                .map(definition -> new com.contentgrid.surveyor.spi.storage.Resource(definition, command.resourceId()))
                .map(resource -> {
                    var groupingConfig = findResourceAggregationConfigurationSpiPort.getBillingAggregationConfiguration(
                            resource.getDefinition(), interval.getDuration());
                    return eventCountMetricSpiPort.getAggregatedEventCountMetric(resource, interval, groupingConfig);
                })
                .collect(Collectors.toMap(this::toResource,
                        metric -> new Metric(
                                metric.getMeasureInterval().getStartTime(),
                                metric.getMeasureInterval().getEndTime(),
                                metric.getValue()
                        )));
    }

    private Resource toResource(EventCountMetric metric) {
        var resource = metric.getResource();
        var definition = resource.getDefinition();
        return new Resource(
                definition.sourceSystem(),
                definition.resourceType(),
                metric.getResource().getResourceId(),
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
}
