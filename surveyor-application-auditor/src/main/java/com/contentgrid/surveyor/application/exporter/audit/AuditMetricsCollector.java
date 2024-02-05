package com.contentgrid.surveyor.application.exporter.audit;


import io.prometheus.metrics.core.metrics.Counter;
import io.prometheus.metrics.model.registry.Collector;
import io.prometheus.metrics.model.snapshots.MetricSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;

@RequiredArgsConstructor
public class AuditMetricsCollector implements MessageReceiver, Collector {

    private final Counter counter = Counter.builder()
            .name("contentgrid.requests")
            .labelNames("response_cat", "entity_name")
            .build();

    @Override
    public void receive(Message<GenericAuditEvent> message) {
        counter.labelValues(message.getPayload().getResponseCategory(), message.getPayload().getDomainType()).inc();
    }

    @Override
    public MetricSnapshot collect() {
        return counter.collect();
    }
}
