package com.contentgrid.surveyor.infrastructure.storage.r2dbc;

import com.contentgrid.surveyor.spi.resources.ResourceIdentity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ResourceIdentityRepository extends ReactiveCrudRepository<ResourceIdentityEntity, Long> {

    @Query("""
            insert into resource_identity(source_system, resource_type, resource_id)
            values(:#{#resource.sourceSystem.sourceName}, :#{#resource.resourceType.resourceType}, :#{#resource.resourceId.resourceId})
            on conflict (source_system, resource_type, resource_id) do update SET source_system=resource_identity.source_system -- needs a dummy update to actually return something
            returning id, source_system, resource_type, resource_id
            """)
    Mono<ResourceIdentityEntity> upsert(ResourceIdentity resource);

    Flux<ResourceIdentityEntity> findAllByLinkOrgRefIsNull();

    @Query("""
            select * from resource_identity ri
                where ri.source_system = :#{#resourceIdentity.sourceSystem.sourceName}
                and ri.resource_type = :#{#resourceIdentity.resourceType.resourceType}
                and ri.resource_id = :#{#resourceIdentity.resourceId.resourceId}
            """)
    Mono<ResourceIdentityEntity> find(ResourceIdentity resourceIdentity);

    default Mono<ResourceIdentityEntity> findOrCreate(ResourceIdentity resourceIdentity) {
        // We do a check before upserting to reduce the number of times a tuple gets changed
        // Since resources can only be created, never removed, this is safe to execute outside a transaction
        // In the "worst" case the resource is created after #find() returns none, and it is updated by the upsert
        return find(resourceIdentity)
                .switchIfEmpty(Mono.defer(() -> upsert(resourceIdentity)));
    }
}
