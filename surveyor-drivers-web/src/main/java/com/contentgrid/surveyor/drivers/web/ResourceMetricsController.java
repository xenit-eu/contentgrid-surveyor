package com.contentgrid.surveyor.drivers.web;

import com.contentgrid.surveyor.api.metrics.FindMetrics;
import com.contentgrid.surveyor.api.metrics.FindMetrics.FindMetricsCommand;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.hateoas.CollectionModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ResourceMetricsController {
    private final FindMetrics findMetrics;

    @GetMapping("/metrics/{system}/{resourceType}/{resourceId}")
    public CollectionModel<MetricRepresentationModel> getMetrics(
            @PathVariable String system,
            @PathVariable String resourceType,
            @PathVariable String resourceId,
            @RequestParam(required = false) Instant start,
            @RequestParam(required = false) Instant end,
            @RequestParam(required = false) Duration step
    ) {
        return CollectionModel.of(findMetrics.findMetrics(FindMetricsCommand.builder()
                        .system(system)
                        .resourceType(resourceType)
                        .resourceId(resourceId)
                        .start(start)
                        .end(end)
                        .step(step)
                .build())
                .entrySet()
                .stream()
                .map(resourceAndMetric -> new MetricRepresentationModel(resourceAndMetric.getKey().metric(), resourceAndMetric.getValue().stream().map(metric -> new MetricRepresentationModel.MetricData(metric.startTime(), metric.endTime(), metric.value())).toList()))
                .toList());
    }

}
