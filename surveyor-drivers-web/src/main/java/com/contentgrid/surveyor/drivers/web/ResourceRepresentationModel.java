package com.contentgrid.surveyor.drivers.web;

import com.contentgrid.surveyor.api.metrics.Resource;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

@Relation(itemRelation = "resource", collectionRelation = "resources")
@Value
@EqualsAndHashCode(callSuper = true)
public class ResourceRepresentationModel extends RepresentationModel<ResourceRepresentationModel> {
    String system;
    String resource;
    String metric;
    String resourceId;

    static ResourceRepresentationModel from(Resource resource) {
        return new ResourceRepresentationModel(
                resource.system().sourceName(),
                resource.resourceType().resourceType(),
                resource.metric().name(),
                resource.resourceId().resourceId()
        );
    }
}
