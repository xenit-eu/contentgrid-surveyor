package com.contentgrid.surveyor.infrastructure.storage.memory;

import com.contentgrid.surveyor.infrastructure.storage.tests.MetricsGatewayTest;
import com.contentgrid.surveyor.spi.storage.AggregateMeasurementsSpiPort;
import com.contentgrid.surveyor.spi.storage.LastMeasurementSpiPort;
import com.contentgrid.surveyor.spi.storage.StoreMeasurementSpiPort;

class InMemoryMeasurementGatewayTest extends MetricsGatewayTest {

    private final InMemoryMeasurementGateway gateway = new InMemoryMeasurementGateway();

    @Override
    protected StoreMeasurementSpiPort getStoreEventCountMetricPort() {
        return gateway;
    }

    @Override
    protected AggregateMeasurementsSpiPort getAggregateEventCountMetricPort() {
        return gateway;
    }

    @Override
    protected LastMeasurementSpiPort getLastEventCountMetricPort() {
        return gateway;
    }

}