package com.contentgrid.surveyor.drivers.web;

import com.contentgrid.surveyor.api.metrics.ExportedMetrics;
import com.contentgrid.surveyor.api.metrics.FindBillingMetrics;
import com.contentgrid.surveyor.api.metrics.FindBillingMetrics.BillingMetricsCommand;
import com.contentgrid.surveyor.api.metrics.FindExportedMetrics;
import com.contentgrid.surveyor.api.metrics.FindExportedMetrics.ExportMetricsCommand;
import com.contentgrid.surveyor.api.metrics.FindInsightMetrics;
import com.contentgrid.surveyor.api.metrics.FindInsightMetrics.FindInsightMetricsCommand;
import com.contentgrid.surveyor.api.metrics.Metric;
import com.contentgrid.surveyor.api.metrics.Resource;
import com.contentgrid.surveyor.drivers.web.MetricRepresentationModel.MetricData;
import com.contentgrid.surveyor.jackson.streaming.generator.DataBufferOutputStream;
import com.contentgrid.surveyor.values.MetricName;
import com.contentgrid.surveyor.values.ResourceId;
import com.contentgrid.surveyor.values.ResourceType;
import com.contentgrid.surveyor.values.SourceName;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.reactivestreams.Subscription;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;

@RestController
@RequiredArgsConstructor
public class ResourceMetricsController {

    private final FindExportedMetrics findExportedMetrics;
    private final FindInsightMetrics findInsightMetrics;
    private final FindBillingMetrics findBillingMetrics;
    private final ObjectMapper objectMapper;

    @GetMapping("/metrics/{resourceType}:{metric}")
    public Mono<Void> exportMetrics(
            @PathVariable String resourceType,
            @PathVariable String metric,
            @RequestParam Instant start,
            @RequestParam Instant end,
            ServerHttpResponse response
    ) {
        ExportMetricsCommand command = ExportMetricsCommand.builder()
                .resourceType(ResourceType.of(resourceType))
                .metric(MetricName.of(metric))
                .start(start)
                .end(end)
                .build();
        var metrics = findExportedMetrics.findMetricsForExport(command);

        var dataBuffers = Flux.<DataBuffer>create(sink -> {
            var outputStream = new DataBufferOutputStream(
                    response.bufferFactory(),
                    sink::next
            );
            try {
                var generator = objectMapper.createGenerator(outputStream);

                generator.writeStartObject();
                generator.writeFieldName("_embedded");
                generator.writeStartObject();
                generator.writeFieldName("metrics");
                generator.writeStartArray();

                Flux.from(metrics)
                        .doOnError(sink::error)
                        .subscribe(new ExportedMetricsWritingSubscriber(generator, sink::complete));
            } catch (IOException ioException) {
                sink.error(ioException);
            }
        });

        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        return response.writeWith(dataBuffers)
                .then(Mono.defer(response::setComplete));

    }

    @GetMapping("/metrics/insights/{system}/{resourceType}/{resourceId}")
    public Mono<CollectionModel<MetricRepresentationModel>> insightMetrics(
            @PathVariable String system,
            @PathVariable String resourceType,
            @PathVariable String resourceId,
            @RequestParam(required = false) Instant start,
            @RequestParam(required = false) Instant end,
            @RequestParam(required = false) Duration step
    ) {
        var command = FindInsightMetricsCommand.builder()
                .system(SourceName.of(system))
                .resourceType(ResourceType.of(resourceType))
                .resourceId(ResourceId.of(resourceId))
                .start(start)
                .end(end)
                .step(step)
                .build();
        return Flux.from(findInsightMetrics.findMetricsForInsights(command))
                .flatMap(m -> Flux.from(m.metrics())
                        .collectList()
                        .map(metrics -> Map.entry(m.resource(), metrics))
                )
                .collectMap(Entry::getKey, Entry::getValue)
                .map(ResourceMetricsController::toMetricRepresentationCollection);
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
    public Mono<CollectionModel<AggregateRepresentationModel>> billingMetrics(
            @PathVariable String system,
            @PathVariable String resourceType,
            @PathVariable String resourceId,
            @RequestParam(required = false) Instant start,
            @RequestParam(required = false) Instant end
    ) {
        var command = BillingMetricsCommand.builder()
                .system(SourceName.of(system))
                .resourceType(ResourceType.of(resourceType))
                .resourceId(ResourceId.of(resourceId))
                .start(start)
                .end(end)
                .build();

        return Flux.from(findBillingMetrics.findMetricsForBilling(command))
                .map(resourceMetric -> new AggregateRepresentationModel(
                        ResourceRepresentationModel.from(resourceMetric.resource()),
                        resourceMetric.metric().startTime(),
                        resourceMetric.metric().endTime(),
                        resourceMetric.metric().value()
                ))
                .collectList()
                .map(CollectionModel::of);
    }

    @RequiredArgsConstructor
    private static class ExportedMetricsWritingSubscriber extends BaseSubscriber<ExportedMetrics> {

        private final JsonGenerator generator;
        private final Runnable finished;

        @Override
        protected void hookOnSubscribe(Subscription subscription) {
            subscription.request(1);
        }

        @Override
        @SneakyThrows
        protected void hookOnNext(ExportedMetrics value) {
            generator.writeStartObject();
            generator.writeFieldName("resource");
            generator.writeObject(ResourceRepresentationModel.from(value.resource()));
            generator.writeFieldName("data");
            generator.writeStartArray();

            value.metrics().subscribe(new MetricWriterSubscriber());
        }

        @Override
        @SneakyThrows
        protected void hookOnComplete() {
            generator.writeEndArray();
            generator.writeEndObject();
            generator.writeEndObject();
        }

        @Override
        @SneakyThrows
        protected void hookFinally(SignalType type) {
            generator.close();
            finished.run();
        }

        private class MetricWriterSubscriber extends BaseSubscriber<Metric> {

            @Override
            @SneakyThrows
            protected void hookOnNext(Metric value) {
                generator.writeObject(new MetricData(value.startTime(), value.endTime(), value.value()));
            }

            @Override
            @SneakyThrows
            protected void hookOnComplete() {
                generator.writeEndArray();
                generator.writeEndObject();
                ExportedMetricsWritingSubscriber.this.upstream().request(1);
            }
        }
    }

}
