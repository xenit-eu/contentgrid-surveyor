package com.contentgrid.surveyor.application.exporter.cgapp;

import java.util.function.Consumer;
import lombok.val;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

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
    MetricsCollector messageReceiver() {
        return new MetricsCollector();
    }

}
