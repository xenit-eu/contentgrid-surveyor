package com.contentgrid.surveyor.application.exporter.audit;

import org.springframework.messaging.Message;

public interface MessageReceiver {
    void receive(Message<?> message);
}
