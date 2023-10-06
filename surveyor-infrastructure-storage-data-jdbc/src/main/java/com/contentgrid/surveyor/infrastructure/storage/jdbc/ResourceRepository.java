package com.contentgrid.surveyor.infrastructure.storage.jdbc;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.QueryByExampleExecutor;

public interface ResourceRepository extends CrudRepository<ResourceRepository, Long>,
        QueryByExampleExecutor<ResourceRepository> {

    @Query("""
            insert into resource(source_system, resource_type, resource_id, metric_name)
            values(:#{#resource.sourceSystem}, :#{#resource.resourceType}, :#{#resource.resourceId}, :#{#resource.metricName})
            on conflict (source_system, resource_type, resource_id, metric_name) do update SET source_system=resource.source_system -- needs a dummy update to actually return something
            returning id, source_system, resource_type, resource_id, metric_name
            """)
    ResourceEntity upsert(ResourceEntity resource);

}
