package com.contentgrid.surveyor.infrastructure.storage.tests;

import static org.assertj.core.api.Assertions.assertThat;

import com.contentgrid.surveyor.spi.ResourceDefinition;
import com.contentgrid.surveyor.spi.resources.CreateMetricSpiPort;
import com.contentgrid.surveyor.spi.resources.FindUnlinkedResourcesSpiPort;
import com.contentgrid.surveyor.spi.resources.LinkResourceSpiPort;
import com.contentgrid.surveyor.spi.resources.LinkResourceSpiPort.ResourceAlreadyLinkedException;
import com.contentgrid.surveyor.spi.resources.LinkResourceSpiPort.ResourceNotFoundException;
import com.contentgrid.surveyor.spi.resources.ResourceIdentity;
import com.contentgrid.surveyor.spi.resources.ResourceLinkage;
import com.contentgrid.surveyor.values.MetricName;
import com.contentgrid.surveyor.values.ResourceId;
import com.contentgrid.surveyor.values.ResourceType;
import com.contentgrid.surveyor.values.SourceName;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

public abstract class ResourceGatewayTest {

    protected abstract CreateMetricSpiPort getCreateMetricPort();

    protected abstract FindUnlinkedResourcesSpiPort getFindUnlinkedResourcesPort();

    protected abstract LinkResourceSpiPort getLinkResourcePort();

    private final static ResourceDefinition RESOURCE_A = new ResourceDefinition(
            SourceName.of("test"),
            ResourceType.of("storage"),
            MetricName.of("stored_bytes")
    );

    private final static ResourceDefinition RESOURCE_B = new ResourceDefinition(
            SourceName.of("test"),
            ResourceType.of("storage"),
            MetricName.of("stored_files")
    );

    private final static ResourceDefinition RESOURCE_C = new ResourceDefinition(
            SourceName.of("testX"),
            ResourceType.of("storage"),
            MetricName.of("stored_files")
    );

    private final static ResourceDefinition RESOURCE_D = new ResourceDefinition(
            SourceName.of("test"),
            ResourceType.of("db"),
            MetricName.of("stored_files")
    );

    @Test
    void findUnlinkedResources() {
        var createResource = getCreateMetricPort();

        var resourceId = ResourceId.of(UUID.randomUUID().toString());

        var metricA = RESOURCE_A.createMetric(resourceId, Map.of());
        var metricB = RESOURCE_B.createMetric(resourceId, Map.of());
        var metricC = RESOURCE_C.createMetric(resourceId, Map.of());
        var metricD = RESOURCE_D.createMetric(resourceId, Map.of());

        Flux.just(metricA, metricB, metricC, metricD)
                .flatMap(createResource::createMetric)
                .blockLast();

        assertThat(getFindUnlinkedResourcesPort().findUnlinkedResources()
                .filter(resourceIdentity -> Objects.equals(resourceIdentity.getResourceId(), resourceId))
                .collect(Collectors.toSet())
                .block())
                .containsExactlyInAnyOrder(
                        new ResourceIdentity(SourceName.of("test"), ResourceType.of("storage"), resourceId),
                        new ResourceIdentity(SourceName.of("testX"), ResourceType.of("storage"), resourceId),
                        new ResourceIdentity(SourceName.of("test"), ResourceType.of("db"), resourceId)
                );

        getLinkResourcePort().linkResource(
                new ResourceIdentity(SourceName.of("test"), ResourceType.of("storage"), resourceId),
                new ResourceLinkage("test", "test", "test")
        ).block();

        assertThat(getFindUnlinkedResourcesPort().findUnlinkedResources()
                .filter(resourceIdentity -> Objects.equals(resourceIdentity.getResourceId(), resourceId))
                .collect(Collectors.toSet())
                .block())
                .containsExactlyInAnyOrder(
                        new ResourceIdentity(SourceName.of("testX"), ResourceType.of("storage"), resourceId),
                        new ResourceIdentity(SourceName.of("test"), ResourceType.of("db"), resourceId)
                );
    }

    @Test
    void linkUnknownResource() {
        var linkResource = getLinkResourcePort();

        StepVerifier.create(linkResource.linkResource(
                        new ResourceIdentity(SourceName.of("test"), ResourceType.of("test"), ResourceId.of("def")),
                        new ResourceLinkage("test", "test", "test")))
                .expectError(ResourceNotFoundException.class)
                .verify();
    }

    @Test
    void linkAlreadyLinkedResource() {
        var createResource = getCreateMetricPort();

        var resourceId = ResourceId.of(UUID.randomUUID().toString());
        var metricA = RESOURCE_A.createMetric(resourceId, Map.of());

        createResource.createMetric(metricA).block();

        getLinkResourcePort().linkResource(
                new ResourceIdentity(SourceName.of("test"), ResourceType.of("storage"), resourceId),
                new ResourceLinkage("test", "test", "test")
        ).block();

        StepVerifier.create(getLinkResourcePort().linkResource(
                        new ResourceIdentity(SourceName.of("test"), ResourceType.of("storage"), resourceId),
                        new ResourceLinkage("test", "test", "test2")))
                .expectError(ResourceAlreadyLinkedException.class)
                .verify();
    }

}
