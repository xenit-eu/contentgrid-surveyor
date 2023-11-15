package com.contentgrid.surveyor.infrastructure.storage.r2dbc;

import com.contentgrid.surveyor.spi.ResourceDefinition;
import com.contentgrid.surveyor.spi.resources.Metric;
import com.contentgrid.surveyor.spi.resources.ResourceIdentity;
import com.contentgrid.surveyor.values.MetricName;
import com.contentgrid.surveyor.values.ResourceId;
import com.contentgrid.surveyor.values.ResourceType;
import com.contentgrid.surveyor.values.SourceName;
import java.util.Map;
import java.util.Objects;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface MetricRepository extends Repository<MetricEntity, Long> {

    @Query("""
            insert into metric(resource_identity_id, metric_name, tags)
            values(:#{#metric.resourceIdentityId}, :#{#metric.metricName}, :#{#metric.tags})
            on conflict (resource_identity_id, metric_name, tags) do update SET resource_identity_id=metric.resource_identity_id -- needs a dummy update to actually return something
            returning id, resource_identity_id, metric_name, tags
            """)
    Mono<MetricEntity> upsert(MetricEntity metric);

    @Query("""
            select m.*, ri.source_system, ri.resource_type, ri.resource_id from metric m
                join resource_identity ri on ri.id = m.resource_identity_id
                where m.metric_name = :#{#resourceDefinition.metricName.name}
                and ri.source_system = :#{#resourceDefinition.sourceSystem.sourceName}
                and ri.resource_type = :#{#resourceDefinition.resourceType.resourceType}
            """)
    Flux<MetricAndResourceIdentityView> findAllByResourceDefinition(
            ResourceDefinition resourceDefinition
    );

    @Query("""
            select m.*, ri.source_system, ri.resource_type, ri.resource_id from metric m
                join resource_identity ri on ri.id = m.resource_identity_id
                where m.metric_name = :#{#metric.metricName.name}
                and m.tags = :#{#metric.tags}
                and ri.source_system = :#{#metric.resourceIdentity.sourceSystem.sourceName}
                and ri.resource_type = :#{#metric.resourceIdentity.resourceType.resourceType}
                and ri.resource_id = :#{#metric.resourceIdentity.resourceId.resourceId}
            """)
    Mono<MetricAndResourceIdentityView> find(Metric metric);

    record MetricAndResourceIdentityView(
            Long id,
            Long resourceIdentityId,
            String metricName,
            Map<String, String> tags,
            String sourceSystem,
            String resourceType,
            String resourceId
    ) {

        public static MetricAndResourceIdentityView from(ResourceIdentityEntity resourceIdentity, MetricEntity metric) {
            if (!Objects.equals(resourceIdentity.getId(), metric.getResourceIdentityId())) {
                throw new IllegalArgumentException("Resource identity and metric must reference each other");
            }
            return new MetricAndResourceIdentityView(
                    metric.getId(),
                    metric.getResourceIdentityId(),
                    metric.getMetricName(),
                    metric.getTags(),
                    resourceIdentity.getSourceSystem(),
                    resourceIdentity.getResourceType(),
                    resourceIdentity.getResourceId()
            );
        }

        public Metric toDomain() {
            return new Metric(
                    new ResourceIdentity(
                            SourceName.of(sourceSystem),
                            ResourceType.of(resourceType),
                            ResourceId.of(resourceId)
                    ),
                    MetricName.of(metricName),
                    tags
            );
        }
    }
}
