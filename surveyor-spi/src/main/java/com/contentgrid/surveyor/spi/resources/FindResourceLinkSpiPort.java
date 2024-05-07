package com.contentgrid.surveyor.spi.resources;

import reactor.core.publisher.Mono;

public interface FindResourceLinkSpiPort {
    Mono<ResourceLinkage> lookupLinkageForResource(ResourceIdentity identity);
}
