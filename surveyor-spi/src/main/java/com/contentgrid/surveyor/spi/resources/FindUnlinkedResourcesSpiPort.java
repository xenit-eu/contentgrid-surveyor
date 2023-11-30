package com.contentgrid.surveyor.spi.resources;

import reactor.core.publisher.Flux;

public interface FindUnlinkedResourcesSpiPort {
    Flux<ResourceIdentity> findUnlinkedResources();

}
