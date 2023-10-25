package com.contentgrid.surveyor.infrastructure.storage.jdbc;

import com.contentgrid.surveyor.spi.storage.Resource;
import com.contentgrid.surveyor.values.MetricName;
import com.contentgrid.surveyor.values.ResourceId;
import com.contentgrid.surveyor.values.ResourceType;
import com.contentgrid.surveyor.values.SourceName;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("resource")
@Getter
@NoArgsConstructor
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ResourceEntity {

    @Id
    Long id;

    @NonNull
    SourceName sourceSystem;

    @NonNull
    ResourceType resourceType;

    @NonNull
    ResourceId resourceId;

    @NonNull
    String metricName;

    public static ResourceEntity from(Resource resource) {
        return ResourceEntity.builder()
                .sourceSystem(resource.getDefinition().sourceSystem())
                .resourceType(resource.getDefinition().resourceType())
                .metricName(resource.getDefinition().metricName().name())
                .resourceId(resource.getResourceId())
                .build();
    }

    public MetricName getMetricName() {
        return MetricName.of(resourceType, metricName);
    }
}
