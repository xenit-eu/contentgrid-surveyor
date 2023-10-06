package com.contentgrid.surveyor.infrastructure.storage.jdbc;

import com.contentgrid.surveyor.spi.ResourceDefinition;
import com.contentgrid.surveyor.spi.TimeInterval;
import com.contentgrid.surveyor.spi.storage.EventCountMetric;
import com.contentgrid.surveyor.spi.storage.LastEventCountMetricSpiPort;
import com.contentgrid.surveyor.spi.storage.Resource;
import com.contentgrid.surveyor.spi.storage.StoreEventCountMetricSpiPort;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DataJdbcMetricsGateway implements StoreEventCountMetricSpiPort, LastEventCountMetricSpiPort {

    private final ResourceRepository resourceRepository;
    private final MetricRepository metricRepository;

    @Override
    public Optional<TimeInterval> getLastEventCountMetricInterval(ResourceDefinition resourceDefinition) {
        return metricRepository.findLast(resourceDefinition)
                .map(metric -> TimeInterval.between(metric.getStartTime(), metric.getEndTime()));
    }

    @Override
    public void storeEventMetrics(List<EventCountMetric> metrics) {
        var resources = new HashMap<Resource, ResourceEntity>();
        for (EventCountMetric metric : metrics) {
            var resource = metric.getResource();

            resources.computeIfAbsent(resource, res -> resourceRepository.upsert(ResourceEntity.from(res)));
        }

        var metricEntities = metrics.stream()
                .map(metric -> MetricEntity.builder()
                        .resourceId(resources.get(metric.getResource()).getId())
                        .startTime(metric.getMeasureInterval().getStartTime())
                        .endTime(metric.getMeasureInterval().getEndTime())
                        .value(metric.getValue())
                        .build())
                .toList();

        metricRepository.saveAll(metricEntities);

    }

    @Override
    public void storeEventMetric(EventCountMetric metric) {
        storeEventMetrics(List.of(metric));
    }
}
