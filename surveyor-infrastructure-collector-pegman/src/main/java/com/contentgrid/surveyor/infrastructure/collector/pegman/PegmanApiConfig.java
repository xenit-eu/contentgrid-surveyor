package com.contentgrid.surveyor.infrastructure.collector.pegman;

import java.net.URI;
import java.util.Map;
import java.util.function.Consumer;
import lombok.NonNull;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient.Builder;

@lombok.Builder
public record PegmanApiConfig(
        @NonNull
        URI url,
        @NonNull
        Map<String, String> headers,
        String username,
        String password,
        String bearer,
        ReactiveOAuth2AuthorizedClientManager authorizedClientManager
) implements Consumer<Builder> {

    @Override
    public void accept(Builder builder) {
        builder.baseUrl(url.toASCIIString())
                .defaultHeaders(httpHeaders -> {
                    if (username != null) {
                        httpHeaders.setBasicAuth(username, password);
                    } else if (bearer != null && authorizedClientManager == null) {
                        httpHeaders.setBearerAuth(bearer);
                    }
                    headers.forEach(httpHeaders::set);
                });

        if (authorizedClientManager != null) {
            var oauth2ClientFilterFunction = new ServerOAuth2AuthorizedClientExchangeFilterFunction(
                    authorizedClientManager);
            oauth2ClientFilterFunction.setDefaultClientRegistrationId(bearer);
            builder.filter(oauth2ClientFilterFunction);
        }
    }
}
