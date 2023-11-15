package com.contentgrid.surveyor.infrastructure.storage.memory;

import com.contentgrid.surveyor.spi.resources.CreateMetricSpiPort;
import com.contentgrid.surveyor.spi.resources.FindUnlinkedResourcesSpiPort;
import com.contentgrid.surveyor.spi.resources.LinkResourceSpiPort;
import com.contentgrid.surveyor.spi.resources.Metric;
import com.contentgrid.surveyor.spi.resources.ResourceIdentity;
import com.contentgrid.surveyor.spi.resources.ResourceLinkage;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.core.io.Resource;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class InMemoryResourceGateway implements LinkResourceSpiPort, FindUnlinkedResourcesSpiPort,
        CreateMetricSpiPort {
    private static final ResourceLinkage SENTINEL = new ResourceLinkage(null, null, null);
    private final Map<ResourceIdentity, ResourceLinkage> memory = new ConcurrentHashMap<>();

    @Override
    public Mono<Void> createMetric(Metric metric) {
        return Mono.fromRunnable(() -> {
            memory.putIfAbsent(metric.getResourceIdentity(), SENTINEL);
        });
    }

    @Override
    public Flux<ResourceIdentity> findUnlinkedResources() {
        return Flux.fromIterable(memory.entrySet())
                .filter(e -> e.getValue() == SENTINEL)
                .map(Entry::getKey);
    }

    @Override
    public Mono<Void> linkResource(ResourceIdentity resource, ResourceLinkage resourceLinkage) {
        return Mono.defer(() -> {
            var result = memory.compute(resource, (res, info) -> {
                if(info == null) {
                    return null;
                }
                if(info == SENTINEL) {
                    return resourceLinkage;
                }
                return SENTINEL;
            });

            if(result == null) {
                return Mono.error(new ResourceNotFoundException(resource));
            }
            if(result == SENTINEL) {
                return Mono.error(new ResourceAlreadyLinkedException(resource));
            }
            return Mono.empty();
        });
    }
}
