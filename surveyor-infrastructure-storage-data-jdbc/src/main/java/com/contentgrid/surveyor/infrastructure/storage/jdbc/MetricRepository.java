package com.contentgrid.surveyor.infrastructure.storage.jdbc;

import com.contentgrid.surveyor.infrastructure.storage.jdbc.MetricEntity.MetricEntityId;
import com.contentgrid.surveyor.spi.ResourceDefinition;
import java.util.Optional;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.Repository;

public interface MetricRepository extends Repository<MetricEntity, MetricEntityId> {

    Iterable<MetricEntity> saveAll(Iterable<MetricEntity> entities);

    @Query("""
            select m.* from metric_events m
                left join resource r on m.resource_id = r.id
                where r.source_system = :#{#definition.sourceSystem.sourceName}
                and r.resource_type = :#{#definition.resourceType.resourceType}
                and r.metric_name = :#{#definition.metricName.name}
                order by m.end_time desc
                limit 1
            """)
    Optional<MetricEntity> findLast(ResourceDefinition definition);
}
