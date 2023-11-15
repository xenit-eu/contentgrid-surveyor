package com.contentgrid.surveyor.infrastructure.storage.r2dbc;

import com.contentgrid.surveyor.infrastructure.storage.tests.ResourceGatewayTest;
import com.contentgrid.surveyor.spi.resources.CreateMetricSpiPort;
import com.contentgrid.surveyor.spi.resources.FindUnlinkedResourcesSpiPort;
import com.contentgrid.surveyor.spi.resources.LinkResourceSpiPort;
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
public class DataR2dbcResourceGatewayTest extends ResourceGatewayTest {

    @Autowired
    private CreateMetricSpiPort createMetricPort;

    @Autowired
    private FindUnlinkedResourcesSpiPort findUnlinkedResourcesPort;

    @Autowired
    private LinkResourceSpiPort linkResourcePort;

}
