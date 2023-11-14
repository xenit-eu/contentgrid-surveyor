package com.contentgrid.surveyor.spi.resources;

import java.util.List;
import reactor.core.publisher.Flux;

public interface FindUnlinkedResourcesSpiPort {
    Flux<ResourceIdentity> findUnlinkedResources();

}
