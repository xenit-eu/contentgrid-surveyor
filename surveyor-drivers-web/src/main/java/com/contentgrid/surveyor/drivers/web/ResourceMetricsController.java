package com.contentgrid.surveyor.drivers.web;

import com.contentgrid.surveyor.api.metrics.BillingMetrics;
import com.contentgrid.surveyor.api.metrics.BillingMetrics.BillingMetricsCommand;
import com.contentgrid.surveyor.api.metrics.ExportMetrics;
import com.contentgrid.surveyor.api.metrics.ExportMetrics.ExportMetricsCommand;
import com.contentgrid.surveyor.api.metrics.FindInsightMetrics;
import com.contentgrid.surveyor.api.metrics.FindInsightMetrics.FindInsightMetricsCommand;
import com.contentgrid.surveyor.api.metrics.Metric;
import com.contentgrid.surveyor.api.metrics.Resource;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.hateoas.CollectionModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ResourceMetricsController {

    private final ExportMetrics exportMetrics;
    private final FindInsightMetrics findInsightMetrics;
    private final BillingMetrics billingMetrics;

    @GetMapping("/metrics/{resourceType}:{metric}")
    public CollectionModel<MetricRepresentationModel> exportMetrics(
            @PathVariable String resourceType,
            @PathVariable String metric,
            @RequestParam Instant start,
            @RequestParam Instant end
    ) {
        ExportMetricsCommand command = ExportMetricsCommand.builder()
                .resourceType(resourceType)
                .metric(metric)
                .start(start)
                .end(end)
                .build();
        var metrics = exportMetrics.findMetricsForExport(command);

        return toMetricRepresentationCollection(metrics);
    }

    @GetMapping("/metrics/insights/{system}/{resourceType}/{resourceId}")
    public CollectionModel<MetricRepresentationModel> insightMetrics(
            @PathVariable String system,
            @PathVariable String resourceType,
            @PathVariable String resourceId,
            @RequestParam(required = false) Instant start,
            @RequestParam(required = false) Instant end,
            @RequestParam(required = false) Duration step
    ) {
        var command = FindInsightMetricsCommand.builder()
                .system(system)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .start(start)
                .end(end)
                .step(step)
                .build();
        var metricsForInsights = findInsightMetrics.findMetricsForInsights(command);
        return toMetricRepresentationCollection(metricsForInsights);
    }

    private static CollectionModel<MetricRepresentationModel> toMetricRepresentationCollection(
            Map<Resource, List<Metric>> metricsForInsights) {
        return CollectionModel.of(metricsForInsights
                .entrySet()
                .stream()
                .map(resourceAndMetric -> new MetricRepresentationModel(
                        ResourceRepresentationModel.from(resourceAndMetric.getKey()),
                        resourceAndMetric.getValue().stream()
                                .map(metric -> new MetricRepresentationModel.MetricData(metric.startTime(),
                                        metric.endTime(), metric.value())).toList()))
                .toList());
    }

    @GetMapping("/metrics/billing/{system}/{resourceType}/{resourceId}")
    public CollectionModel<AggregateRepresentationModel> billingMetrics(
            @PathVariable String system,
            @PathVariable String resourceType,
            @PathVariable String resourceId,
            @RequestParam(required = false) Instant start,
            @RequestParam(required = false) Instant end
    ) {
        return CollectionModel.of(billingMetrics.findMetricsForBilling(BillingMetricsCommand.builder()
                        .system(system)
                        .resourceType(resourceType)
                        .resourceId(resourceId)
                        .start(start)
                        .end(end)
                        .build())
                .entrySet()
                .stream()
                .map(resourceAndMetric -> new AggregateRepresentationModel(
                        ResourceRepresentationModel.from(resourceAndMetric.getKey()),
                        resourceAndMetric.getValue()
                                .startTime(), resourceAndMetric.getValue().endTime(),
                        resourceAndMetric.getValue().value())
                ).toList()
        );
    }
}
