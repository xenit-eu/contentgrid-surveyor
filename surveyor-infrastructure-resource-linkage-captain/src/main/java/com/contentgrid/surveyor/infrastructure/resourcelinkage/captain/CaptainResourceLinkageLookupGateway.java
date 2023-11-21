package com.contentgrid.surveyor.infrastructure.resourcelinkage.captain;

import com.contentgrid.surveyor.spi.resources.LookupResourceLinkSpiPort;
import com.contentgrid.surveyor.spi.resources.ResourceIdentity;
import com.contentgrid.surveyor.spi.resources.ResourceLinkage;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.config.HypermediaWebClientConfigurer;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.Builder;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class CaptainResourceLinkageLookupGateway implements LookupResourceLinkSpiPort {
    @NonNull
    private final WebClient webClient;

    public CaptainResourceLinkageLookupGateway(
            WebClient.Builder clientBuilder,
            CaptainApiConfig apiConfig,
            HypermediaWebClientConfigurer webClientConfigurer
    ) {
        this(
                configureClient(clientBuilder, apiConfig.andThen(webClientConfigurer::registerHypermediaTypes))
        );
    }
    private static WebClient configureClient(WebClient.Builder clientBuilder, Consumer<Builder> configurer) {
        var builder = clientBuilder.clone();
        configurer.accept(builder);
        return builder.build();
    }

    @Override
    public Mono<ResourceLinkage> lookupLinkageForResource(ResourceIdentity identity) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder.build(Map.of(
                        "sourceSystem", identity.getSourceSystem().sourceName(),
                        "resourceType", identity.getResourceType().resourceType(),
                        "resourceId", identity.getResourceId().resourceId()
                )))
                .exchangeToMono(response -> {
                    if(response.statusCode().isError()) {
                        return response.createError();
                    }
                    return response.bodyToMono(new ParameterizedTypeReference<RepresentationModel<?>>() {
                    });
                })
                .map(representationModel -> new ResourceLinkage(
                        toReference(representationModel.getLink("application")),
                        toReference(representationModel.getLink("project")),
                        toReference(representationModel.getRequiredLink("organization"))
                ));
    }

    private String toReference(Link link) {
        return link.toUri().getPath();
    }

    private String toReference(Optional<Link> link) {
        return link.map(this::toReference).orElse(null);
    }
}
