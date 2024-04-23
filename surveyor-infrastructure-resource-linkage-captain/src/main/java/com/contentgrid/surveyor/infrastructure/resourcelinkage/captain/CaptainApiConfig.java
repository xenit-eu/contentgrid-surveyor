package com.contentgrid.surveyor.infrastructure.resourcelinkage.captain;

import java.util.function.Consumer;
import lombok.Builder;
import lombok.NonNull;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

@Builder
public record CaptainApiConfig(
        @NonNull
        String url,
        String bearer,
        ReactiveOAuth2AuthorizedClientManager authorizedClientManager
) implements Consumer<WebClient.Builder> {

    @Override
    public void accept(WebClient.Builder builder) {
        builder.baseUrl(url)
                .defaultHeaders(httpHeaders -> {
                    if (bearer != null && authorizedClientManager == null) {
                        httpHeaders.setBearerAuth(bearer);
                    }
                });

        if (authorizedClientManager != null) {
            var oauth2ClientFilterFunction = new ServerOAuth2AuthorizedClientExchangeFilterFunction(
                    authorizedClientManager);
            oauth2ClientFilterFunction.setDefaultClientRegistrationId(bearer);
            builder.filter(oauth2ClientFilterFunction);
        }
    }
}
