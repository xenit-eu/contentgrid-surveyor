package com.contentgrid.surveyor.infrastructure.storage.jdbc;

import com.contentgrid.surveyor.infrastructure.storage.tests.MetricsGatewayTest;
import com.contentgrid.surveyor.spi.storage.AggregateMeasurementsSpiPort;
import com.contentgrid.surveyor.spi.storage.LastMeasurementSpiPort;
import com.contentgrid.surveyor.spi.storage.StoreMeasurementSpiPort;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataJdbcTest
@Testcontainers
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Getter
class DataJdbcMeasurementGatewayTest extends MetricsGatewayTest {

    @Autowired
    private StoreMeasurementSpiPort storeEventCountMetricPort;
    @Autowired
    private AggregateMeasurementsSpiPort aggregateEventCountMetricPort;
    @Autowired
    private LastMeasurementSpiPort lastEventCountMetricPort;

}