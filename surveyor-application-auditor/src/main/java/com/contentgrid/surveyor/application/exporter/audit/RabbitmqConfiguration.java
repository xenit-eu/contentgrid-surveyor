package com.contentgrid.surveyor.application.exporter.audit;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.prometheus.metrics.exporter.common.PrometheusScrapeHandler;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import java.util.function.Consumer;
import lombok.val;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.web.bind.annotation.RestController;

@Configuration
@ConditionalOnProperty(value = "surveyor.auditor.rabbitmq.enabled")
public class RabbitmqConfiguration {

    @Bean
    MessageConverter jacksonMessageConverter() {
        val converter = new Jackson2JsonMessageConverter();
        converter.setAlwaysConvertToInferredType(true);
        return converter;
    }

    @Bean
    Consumer<?> rabbitListener(MessageReceiver receiver) {
        return new Consumer<Message<GenericAuditEvent>>() {
            @RabbitListener(queues = "${surveyor.auditor.rabbitmq.queue:surveyor-auditor}")
            public void accept(Message<GenericAuditEvent> msg) {
                receiver.receive(msg);
            }
        };
    }

    @Bean
    PrometheusRegistry myMeterRegistry(AuditMetricsCollector auditMetricsCollector) {
        var registry = new PrometheusRegistry();
        registry.register(auditMetricsCollector);
        return registry;
    }

    @Bean
    PrometheusScrapeHandler scrapeHandler(PrometheusRegistry registry) {
        return new PrometheusScrapeHandler(registry);
    }
//    AuditController myController(PrometheusRegistry registry) {
//        return new AuditController(new PrometheusScrapeHandler(registry));
//    }

    @Bean
    AuditMetricsCollector messageReceiver() {
        return new AuditMetricsCollector();
    }

}
