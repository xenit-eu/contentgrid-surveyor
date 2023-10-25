package com.contentgrid.surveyor.infrastructure.storage.jdbc;

import com.contentgrid.surveyor.infrastructure.storage.tests.MetricsGatewayTest;
import com.contentgrid.surveyor.spi.storage.AggregateEventCountMetricSpiPort;
import com.contentgrid.surveyor.spi.storage.LastEventCountMetricSpiPort;
import com.contentgrid.surveyor.spi.storage.StoreEventCountMetricSpiPort;
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
class DataJdbcMetricsGatewayTest extends MetricsGatewayTest {

    @Autowired
    private StoreEventCountMetricSpiPort storeEventCountMetricPort;
    @Autowired
    private AggregateEventCountMetricSpiPort aggregateEventCountMetricPort;
    @Autowired
    private LastEventCountMetricSpiPort lastEventCountMetricPort;

}