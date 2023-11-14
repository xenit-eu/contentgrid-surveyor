package com.contentgrid.surveyor.infrastructure.storage.jdbc;

import com.contentgrid.surveyor.infrastructure.storage.jdbc.MeasurementEntity.MeasurementEntityId;
import com.contentgrid.surveyor.spi.ResourceDefinition;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface MeasurementRepository extends ReactiveCrudRepository<MeasurementEntity, MeasurementEntityId> {

    @Query("""
            select m.* from measurement m
                join metric mt on m.metric_id = mt.id
                join resource_identity r on mt.resource_identity_id = r.id
                where r.source_system = :#{#definition.sourceSystem.sourceName}
                and r.resource_type = :#{#definition.resourceType.resourceType}
                and mt.metric_name = :#{#definition.metricName.name}
                order by m.end_time desc
                limit 1
            """)
    Mono<MeasurementEntity> findLast(ResourceDefinition definition);
}
