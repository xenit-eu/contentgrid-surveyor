package com.contentgrid.surveyor.infrastructure.storage.jdbc;

import com.contentgrid.surveyor.values.MetricName;
import com.contentgrid.surveyor.values.SourceName;
import java.util.stream.Stream;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;

public interface ResourceRepository extends CrudRepository<ResourceRepository, Long> {

    @Query("""
            insert into resource(source_system, resource_type, resource_id, metric_name)
            values(:#{#resource.sourceSystem}, :#{#resource.resourceType}, :#{#resource.resourceId}, :#{#resource.metricName})
            on conflict (source_system, resource_type, resource_id, metric_name) do update SET source_system=resource.source_system -- needs a dummy update to actually return something
            returning id, source_system, resource_type, resource_id, metric_name
            """)
    ResourceEntity upsert(ResourceEntity resource);

    @Query("""
            select * from resource
                where
                    source_system = :#{#sourceSystem}
                    and resource_type = :#{metricName.type}
                    and metric_name = :#{metricName}
            """)
    Stream<ResourceEntity> findAllBySourceSystemAndMetricName(
            SourceName sourceSystem,
            MetricName metricName
    );

}
