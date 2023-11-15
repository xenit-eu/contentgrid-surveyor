package com.contentgrid.surveyor.infrastructure.resourcelinkage.captain;

import java.util.function.Consumer;
import lombok.Builder;
import lombok.NonNull;
import org.springframework.web.reactive.function.client.WebClient;

@Builder
public record CaptainApiConfig(
        @NonNull
        String url
) implements Consumer<WebClient.Builder> {

    @Override
    public void accept(WebClient.Builder builder) {
        builder.baseUrl(url);
    }
}
