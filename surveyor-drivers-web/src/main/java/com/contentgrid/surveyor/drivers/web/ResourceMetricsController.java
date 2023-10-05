package com.contentgrid.surveyor.drivers.web;

import com.contentgrid.surveyor.api.metrics.BillingMetrics;
import com.contentgrid.surveyor.api.metrics.BillingMetrics.BillingMetricsCommand;
import com.contentgrid.surveyor.api.metrics.FindInsightMetrics;
import com.contentgrid.surveyor.api.metrics.FindInsightMetrics.FindInsightMetricsCommand;
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

    private final FindInsightMetrics findInsightMetrics;
    private final BillingMetrics billingMetrics;

    @GetMapping("/metrics/insights/{system}/{resourceType}/{resourceId}")
    public CollectionModel<MetricRepresentationModel> insightMetrics(
            @PathVariable String system,
            @PathVariable String resourceType,
            @PathVariable String resourceId,
            @RequestParam(required = false) Instant start,
            @RequestParam(required = false) Instant end,
            @RequestParam(required = false) Duration step
    ) {
        return CollectionModel.of(findInsightMetrics.findMetricsForInsights(FindInsightMetricsCommand.builder()
                        .system(system)
                        .resourceType(resourceType)
                        .resourceId(resourceId)
                        .start(start)
                        .end(end)
                        .step(step)
                        .build())
                .entrySet()
                .stream()
                .map(resourceAndMetric -> new MetricRepresentationModel(resourceAndMetric.getKey().metric(),
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
                .map(resourceAndMetric -> new AggregateRepresentationModel(resourceAndMetric.getKey().metric(),
                        resourceAndMetric.getValue()
                                .startTime(), resourceAndMetric.getValue().endTime(),
                        resourceAndMetric.getValue().value())
                ).toList()
        );
    }
}
