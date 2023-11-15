package com.contentgrid.surveyor.infrastructure.storage.r2dbc;

import com.contentgrid.surveyor.infrastructure.storage.tests.MetricsGatewayTest;
import com.contentgrid.surveyor.spi.storage.AggregateMeasurementsSpiPort;
import com.contentgrid.surveyor.spi.storage.LastMeasurementSpiPort;
import com.contentgrid.surveyor.spi.storage.StoreMeasurementSpiPort;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataR2dbcTest
@Testcontainers
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Getter
class DataR2dbcMeasurementGatewayTest extends MetricsGatewayTest {

    @Autowired
    private StoreMeasurementSpiPort storeEventCountMetricPort;
    @Autowired
    private AggregateMeasurementsSpiPort aggregateEventCountMetricPort;
    @Autowired
    private LastMeasurementSpiPort lastEventCountMetricPort;

}