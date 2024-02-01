package com.contentgrid.surveyor.application.exporter.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.function.Consumer;
import lombok.val;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;

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
    MessageReceiver messageReceiver() {
        return new MetricsCollector();
    }

}
