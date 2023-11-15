package com.contentgrid.surveyor.usecase.resources;

import com.contentgrid.surveyor.api.resources.LinkResources;
import com.contentgrid.surveyor.spi.resources.FindUnlinkedResourcesSpiPort;
import com.contentgrid.surveyor.spi.resources.LinkResourceSpiPort;
import com.contentgrid.surveyor.spi.resources.LookupResourceLinkSpiPort;
import com.contentgrid.surveyor.spi.resources.ResourceIdentity;
import com.contentgrid.surveyor.spi.resources.ResourceLinkage;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class LinkResourcesUseCase implements LinkResources {
    private final FindUnlinkedResourcesSpiPort findUnlinkedResourcesSpiPort;
    private final LinkResourceSpiPort linkResourceSpiPort;
    private final LookupResourceLinkSpiPort lookupResourceLinkSpiPort;

    @Override
    public void linkUnlinkedResources() {
        findUnlinkedResourcesSpiPort.findUnlinkedResources()
                .flatMap(resourceIdentity -> lookupResourceLinkSpiPort.lookupLinkageForResource(resourceIdentity)
                                .map(linkage -> new ResourceWithLinkage(resourceIdentity, linkage))
                                .doOnError(error -> log.warn("Failed to retrieve linkage for resource {}", resourceIdentity, error))
                                .onErrorComplete()
                        , 10)
                .concatMap(resourceWithLinkage -> linkResourceSpiPort.linkResource(resourceWithLinkage.resourceIdentity(), resourceWithLinkage.linkage)
                        .doOnError(error -> log.warn("Failed to link resource {}", resourceWithLinkage.resourceIdentity(), error))
                                .onErrorComplete()
                )
                .doOnError(error -> log.error("Linking resources errored", error))
                .then()
                .block();
    }

    record ResourceWithLinkage(
            @NonNull
            ResourceIdentity resourceIdentity,
            @NonNull
            ResourceLinkage linkage
    ) {

    }
}
