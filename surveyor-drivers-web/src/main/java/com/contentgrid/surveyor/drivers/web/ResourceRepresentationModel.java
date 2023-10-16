package com.contentgrid.surveyor.drivers.web;

import com.contentgrid.surveyor.api.metrics.Resource;
import lombok.Value;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

@Relation(itemRelation = "resource", collectionRelation = "resources")
@Value
public class ResourceRepresentationModel extends RepresentationModel<ResourceRepresentationModel> {
    String system;
    String resource;
    String metric;
    String resourceId;

    static ResourceRepresentationModel from(Resource resource) {
        return new ResourceRepresentationModel(
                resource.system(),
                resource.resource(),
                resource.metric(),
                resource.resourceId()
        );
    }
}
