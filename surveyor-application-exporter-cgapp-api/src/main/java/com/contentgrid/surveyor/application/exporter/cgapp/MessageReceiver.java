package com.contentgrid.surveyor.application.exporter.cgapp;

import org.springframework.messaging.Message;

public interface MessageReceiver {
    void receive(Message<GenericAuditEvent> message);
}
