package com.contentgrid.surveyor.application.exporter.cgapp;


import io.prometheus.metrics.core.metrics.Counter;
import io.prometheus.metrics.model.registry.Collector;
import io.prometheus.metrics.model.snapshots.MetricSnapshot;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.scheduling.annotation.Scheduled;

@Slf4j
@RequiredArgsConstructor
public class MetricsCollector implements MessageReceiver, Collector {

    private final AtomicReference<Counter> counter = new AtomicReference<>(makeCounter());
    private final AtomicBoolean shouldReset = new AtomicBoolean(false);

    private static Counter makeCounter() {
        return Counter.builder()
                .name("contentgrid.api.requests")
                // Make sure these labels keep matching the values in the receive method
                .labelNames(
                        "application_id",
                        "deployment_id",
                        "response_cat",
                        "entity_name",
                        "operation"
                )
                .build();
    }

    @Override
    public void receive(Message<GenericAuditEvent> message) {
        // Make sure these values keep matching the labels in the Counter's definition,
        // otherwise it will compile but blow up at runtime
        counter.get().labelValues(
                antiNull(message.getHeaders().get("applicationId", String.class), "application_id"),
                antiNull(message.getHeaders().get("deploymentId", String.class), "deployment_id"),
                antiNull(message.getPayload().getResponseCategory(), "response_status_series"),
                antiNull(message.getPayload().getDomainType(), "entity_name"),
                antiNull(message.getPayload().getOperation(), "operation")
        ).inc();
    }

    // Reset counters midnight utc to avoid publishing a big list of old, unused labels
    @Scheduled(cron = "0 0 0 * * ?", zone = "UTC")
    public void scheduleReset() {
        log.info("Scheduling reset of the counter");
        shouldReset.set(true);
    }

    @Override
    public MetricSnapshot collect() {
        var snapshot = counter.get().collect();
        if (shouldReset.compareAndExchange(true, false)) {
            log.info("Resetting counter");
            counter.set(makeCounter());
        }
        return snapshot;
    }

    private static String antiNull(String value, String desc) {
        if (value == null) {
            log.warn("Replacing null with \"(null)\" for property {}", desc);
            return "(null)";
        }
        return value;
    }

}
