package com.contentgrid.surveyor.application.exporter.audit;


import org.springframework.messaging.Message;

public class MetricsCollector implements MessageReceiver {
    private int count = 0;

    @Override
    public void receive(Message<?> message) {
        count++;
        System.out.println(count);
    }

    public int getCount() {
        return count;
    }
}
