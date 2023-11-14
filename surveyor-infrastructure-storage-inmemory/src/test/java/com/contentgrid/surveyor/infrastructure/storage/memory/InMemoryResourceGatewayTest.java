package com.contentgrid.surveyor.infrastructure.storage.memory;

import com.contentgrid.surveyor.infrastructure.storage.tests.ResourceGatewayTest;
import com.contentgrid.surveyor.spi.resources.CreateMetricSpiPort;
import com.contentgrid.surveyor.spi.resources.FindUnlinkedResourcesSpiPort;
import com.contentgrid.surveyor.spi.resources.LinkResourceSpiPort;

public class InMemoryResourceGatewayTest extends ResourceGatewayTest {
    private final InMemoryResourceGateway inMemoryResourceGateway = new InMemoryResourceGateway();


    @Override
    protected CreateMetricSpiPort getCreateMetricPort() {
        return inMemoryResourceGateway;
    }

    @Override
    protected FindUnlinkedResourcesSpiPort getFindUnlinkedResourcesPort() {
        return inMemoryResourceGateway;
    }

    @Override
    protected LinkResourceSpiPort getLinkResourcePort() {
        return inMemoryResourceGateway;
    }
}
