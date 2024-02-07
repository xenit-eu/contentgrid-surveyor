package com.contentgrid.surveyor.application.exporter.audit;


import io.prometheus.metrics.core.metrics.Counter;
import io.prometheus.metrics.model.registry.Collector;
import io.prometheus.metrics.model.snapshots.MetricSnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;

@Slf4j
@RequiredArgsConstructor
public class AuditMetricsCollector implements MessageReceiver, Collector {

    private final Counter counter = Counter.builder()
            .name("contentgrid.requests")
            // Make sure these labels keep matching the values in the receive method
            .labelNames(
                    "application_id",
                    "deployment_id",
                    "response_cat",
                    "entity_name",
                    "operation"
            )
            .build();

    @Override
    public void receive(Message<GenericAuditEvent> message) {
        // Make sure these values keep matching the labels in the Counter's definition,
        // otherwise it will compile but blow up at runtime
        counter.labelValues(
                antiNull(message.getHeaders().get("applicationId", String.class), "application_id"),
                antiNull(message.getHeaders().get("deploymentId", String.class), "deployment_id"),
                antiNull(message.getPayload().getResponseCategory(), "response_cat"),
                antiNull(message.getPayload().getDomainType(), "entity_name"),
                antiNull(message.getPayload().getOperation(), "operation")
        ).inc();
    }

    @Override
    public MetricSnapshot collect() {
        return counter.collect();
    }

    private static String antiNull(String value, String desc) {
        if (value == null) {
            log.warn("Replacing null with \"(null)\" for property {}", desc);
            return "(null)";
        }
        return value;
    }

}
