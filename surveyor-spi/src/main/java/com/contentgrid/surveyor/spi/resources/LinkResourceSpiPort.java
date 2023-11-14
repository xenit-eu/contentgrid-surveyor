package com.contentgrid.surveyor.spi.resources;

import lombok.Getter;
import reactor.core.publisher.Mono;

public interface LinkResourceSpiPort {
    Mono<Void> linkResource(ResourceIdentity resource, ResourceLinkage resourceLinkage);

    @Getter
    class ResourceNotFoundException extends Exception {
        private final ResourceIdentity resource;

        public ResourceNotFoundException(ResourceIdentity resource) {
            super("ResourceIdentity '%s' does not exist".formatted(resource));
            this.resource = resource;
        }
    }

    @Getter
    class ResourceAlreadyLinkedException extends Exception {
        private final ResourceIdentity resource;

        public ResourceAlreadyLinkedException(ResourceIdentity resource) {
            super("ResourceIdentity '%s' is already linked".formatted(resource));
            this.resource = resource;
        }
    }
}
