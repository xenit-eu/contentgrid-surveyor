package com.contentgrid.surveyor.infrastructure.storage.jdbc;

import com.contentgrid.surveyor.spi.resources.ResourceIdentity;
import com.contentgrid.surveyor.spi.resources.ResourceLinkage;
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

@Table("resource_identity")
@Getter
@NoArgsConstructor
@Builder(access = AccessLevel.PACKAGE, toBuilder = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ResourceIdentityEntity {

    @Id
    Long id;

    @NonNull
    String sourceSystem;

    @NonNull
    String resourceType;

    @NonNull
    String resourceId;

    String orgRef;

    String projectRef;

    String applicationRef;

    static class ResourceIdentityEntityBuilder {

        public ResourceIdentityEntityBuilder fromDomain(ResourceIdentity resourceIdentity) {
            return this
                    .sourceSystem(resourceIdentity.getSourceSystem().sourceName())
                    .resourceType(resourceIdentity.getResourceType().resourceType())
                    .resourceId(resourceIdentity.getResourceId().resourceId());
        }

        public ResourceIdentityEntityBuilder fromDomain(ResourceLinkage resourceLinkage) {
            return this
                    .orgRef(resourceLinkage.getOrgRef())
                    .projectRef(resourceLinkage.getProjectRef())
                    .applicationRef(resourceLinkage.getApplicationRef());
        }
    }

    public ResourceIdentity toResourceIdentity() {
        return new ResourceIdentity(
                SourceName.of(sourceSystem),
                ResourceType.of(resourceType),
                ResourceId.of(resourceId)
        );
    }

    public ResourceLinkage toResourceLinkage() {
        return new ResourceLinkage(
                orgRef,
                projectRef,
                applicationRef
        );
    }
}
