package com.contentgrid.surveyor.spi.resources;

import reactor.core.publisher.Mono;

public interface LookupResourceLinkSpiPort {
    Mono<ResourceLinkage> lookupLinkageForResource(ResourceIdentity identity);
}
