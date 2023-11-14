package com.contentgrid.surveyor.infrastructure.storage.jdbc;

import com.contentgrid.surveyor.infrastructure.storage.jdbc.MetricRepository.MetricAndResourceIdentityView;
import com.contentgrid.surveyor.spi.resources.CreateMetricSpiPort;
import com.contentgrid.surveyor.spi.resources.FindUnlinkedResourcesSpiPort;
import com.contentgrid.surveyor.spi.resources.LinkResourceSpiPort;
import com.contentgrid.surveyor.spi.resources.Metric;
import com.contentgrid.surveyor.spi.resources.ResourceIdentity;
import com.contentgrid.surveyor.spi.resources.ResourceLinkage;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class DataJdbcResourceGateway implements CreateMetricSpiPort, FindUnlinkedResourcesSpiPort,
        LinkResourceSpiPort {
    private final ResourceIdentityRepository resourceIdentityRepository;
    private final MetricRepository metricRepository;

    @Override
    public Flux<ResourceIdentity> findUnlinkedResources() {
        return resourceIdentityRepository.findAllByOrgRefIsNull()
                .map(ResourceIdentityEntity::toResourceIdentity);
    }

    @Override
    @Transactional
    public Mono<Void> linkResource(ResourceIdentity resource, ResourceLinkage resourceLinkage) {
        return resourceIdentityRepository.find(resource)
                .switchIfEmpty(Mono.error(() -> new ResourceNotFoundException(resource)))
                .flatMap(resourceIdentityEntity -> {
                    if(resourceIdentityEntity.getOrgRef() != null) {
                        return Mono.error(new ResourceAlreadyLinkedException(resource));
                    }
                    return Mono.just(resourceIdentityEntity.toBuilder()
                            // Add in resource linkage information
                            .fromDomain(resourceLinkage)
                            .build());
                })
                .map(resourceIdentityRepository::save)
                .then();
    }

    @Override
    public Mono<Void> createMetric(Metric metric) {
        return resourceIdentityRepository
                .findOrCreate(metric.getResourceIdentity())
                .flatMap(resourceIdentity -> metricRepository.upsert(MetricEntity.from(resourceIdentity.getId(), metric)))
                .then();
    }
}
