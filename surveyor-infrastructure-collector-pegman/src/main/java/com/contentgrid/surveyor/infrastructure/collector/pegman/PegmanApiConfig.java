package com.contentgrid.surveyor.infrastructure.collector.pegman;

import java.net.URI;
import java.util.Map;
import java.util.function.Consumer;
import lombok.NonNull;
import org.springframework.web.reactive.function.client.WebClient.Builder;

@lombok.Builder
public record PegmanApiConfig(
        @NonNull
        URI url,
        @NonNull
        Map<String, String> headers,
        String username,
        String password,
        String bearer
) implements Consumer<Builder> {

    @Override
    public void accept(Builder builder) {
        builder.baseUrl(url.toASCIIString())
                .defaultHeaders(httpHeaders -> {
                    if (username != null) {
                        httpHeaders.setBasicAuth(username, password);
                    } else if (bearer != null) {
                        httpHeaders.setBearerAuth(bearer);
                    }
                    headers.forEach(httpHeaders::set);
                });

    }
}
