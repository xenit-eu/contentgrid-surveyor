package com.contentgrid.surveyor.infrastructure.storage.tests;

import com.contentgrid.surveyor.spi.ResourceDefinition;
import com.contentgrid.surveyor.spi.resources.CreateMetricSpiPort;
import com.contentgrid.surveyor.spi.resources.FindUnlinkedResourcesSpiPort;
import com.contentgrid.surveyor.spi.resources.LinkResourceSpiPort;
import com.contentgrid.surveyor.spi.resources.Metric;
import com.contentgrid.surveyor.values.MetricName;
import com.contentgrid.surveyor.values.ResourceId;
import com.contentgrid.surveyor.values.ResourceType;
import com.contentgrid.surveyor.values.SourceName;
import java.util.Map;
import org.junit.jupiter.api.Test;

public abstract class ResourceGatewayTest {
    abstract CreateMetricSpiPort getCreateResourcePort();
    abstract FindUnlinkedResourcesSpiPort getFindUnlinkedResourcesPort();
    abstract LinkResourceSpiPort getLinkResourcePort();

    private final static ResourceDefinition RESOURCE_A = new ResourceDefinition(
            SourceName.of("test"),
            ResourceType.of("storage"),
            MetricName.of( "stored_bytes")
    );

    private final static ResourceDefinition RESOURCE_B = new ResourceDefinition(
            SourceName.of("test"),
            ResourceType.of("storage"),
            MetricName.of( "stored_files")
    );

    @Test
    void findUnlinkedResources() {
        var createResource = getCreateResourcePort();

        var resourceId = ResourceId.of("abc");

        var metric = RESOURCE_A.createMetric(resourceId, Map.of());
        createResource.createMetric(metric);
    }

}
