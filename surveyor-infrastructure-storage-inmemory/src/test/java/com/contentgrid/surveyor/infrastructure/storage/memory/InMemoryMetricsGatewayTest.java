package com.contentgrid.surveyor.infrastructure.storage.memory;

import com.contentgrid.surveyor.infrastructure.storage.tests.MetricsGatewayTest;
import com.contentgrid.surveyor.spi.storage.AggregateEventCountMetricSpiPort;
import com.contentgrid.surveyor.spi.storage.LastEventCountMetricSpiPort;
import com.contentgrid.surveyor.spi.storage.StoreEventCountMetricSpiPort;

class InMemoryMetricsGatewayTest extends MetricsGatewayTest {

    private final InMemoryMetricsGateway gateway = new InMemoryMetricsGateway();

    @Override
    protected StoreEventCountMetricSpiPort getStoreEventCountMetricPort() {
        return gateway;
    }

    @Override
    protected AggregateEventCountMetricSpiPort getAggregateEventCountMetricPort() {
        return gateway;
    }

    @Override
    protected LastEventCountMetricSpiPort getLastEventCountMetricPort() {
        return gateway;
    }

}