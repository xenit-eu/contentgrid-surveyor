package com.contentgrid.surveyor.infrastructure.storage.jdbc;

import com.contentgrid.surveyor.spi.storage.Resource;
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
    String sourceSystem;

    @NonNull
    String resourceType;

    @NonNull
    String resourceId;

    @NonNull
    String metricName;

    public static ResourceEntity from(Resource resource) {
        return ResourceEntity.builder()
                .sourceSystem(resource.getDefinition().sourceSystem())
                .resourceType(resource.getDefinition().resourceType())
                .metricName(resource.getDefinition().metricName())
                .resourceId(resource.getResourceId())
                .build();
    }
}
