package com.contentgrid.surveyor.usecase.metrics;

import com.contentgrid.surveyor.api.metrics.FindMetrics;
import com.contentgrid.surveyor.spi.TimeInterval;
import com.contentgrid.surveyor.spi.config.FindResourceDefinitionsSpiPort;
import com.contentgrid.surveyor.spi.storage.AggregateEventCountMetricSpiPort;
import com.contentgrid.surveyor.spi.storage.AggregateEventCountMetricSpiPort.GroupingConfiguration;
import com.contentgrid.surveyor.spi.storage.EventCountMetric;
import com.contentgrid.surveyor.spi.storage.Resource;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class FindMetricsUseCase implements FindMetrics {
    private final Map<GroupingKey, GroupingConfiguration> groupingConfigurations;
    private final AggregateEventCountMetricSpiPort eventCountMetricSpiPort;
    private final FindResourceDefinitionsSpiPort findResourceDefinitionsSpiPort;

    @Override
    public Map<Resource, List<Metric>> findMetrics(FindMetricsCommand command) {
        var definitions = findResourceDefinitionsSpiPort.findResourceDefinitions(command.system(), command.resourceType());

        var thisDay = Instant.now().truncatedTo(ChronoUnit.DAYS);

        var start = Optional.ofNullable(command.start()).orElse(thisDay);
        var end = Optional.ofNullable(command.end()).orElseGet(() -> start.plus(1, ChronoUnit.DAYS));
        var step = Optional.ofNullable(command.step()).orElseGet(() -> Duration.between(start, end).toDays() > 2?Duration.ofDays(1):Duration.ofHours(1));

        return definitions.stream()
                .map(definition -> new com.contentgrid.surveyor.spi.storage.Resource(definition, command.resourceId()))
                .flatMap(resource -> {
                    var groupingConfig = groupingConfigurations.get(new GroupingKey(resource.getDefinition().resourceType(), resource.getDefinition().metricName()));
                    return eventCountMetricSpiPort.findEventCountMetrics(
                            resource,
                            TimeInterval.between(start, end),
                            List.of(
                                    GroupingConfiguration.builder()
                                            .groupInterval(step)
                                            .operation(groupingConfig.operation())
                                            .build()
                            )
                    ).stream();
                })
                .collect(Collectors.groupingBy(metric -> new Resource(metric.getResource().getDefinition().metricName()), Collectors.mapping(metric -> new Metric(
                        metric.getMeasureInterval().getStartTime(),
                        metric.getMeasureInterval().getEndTime(),
                        metric.getResource().getDefinition().metricName(),
                        metric.getValue()
                ), Collectors.toList())));
    }

    public record GroupingKey(
            String resourceType,
            String metric
    ) {

    }
}
