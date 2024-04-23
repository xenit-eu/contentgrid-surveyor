package com.contentgrid.surveyor.infrastructure.resourcelinkage.captain;


import lombok.Data;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.hateoas.config.HypermediaWebClientConfigurer;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration(proxyBeanMethods = false)
public class SurveyorResourceLinkageCaptainConfiguration {
    @Bean
    @ConfigurationProperties(prefix = "surveyor.resource-linkage.captain")
    CaptainApiProperties resourceLinkageCaptainApiConfig() {
        return new CaptainApiProperties();
    }

    @Bean
    CaptainResourceLinkageLookupGateway captainResourceLinkageLookupGateway(
            WebClient.Builder webclientBuilder,
            HypermediaWebClientConfigurer webClientConfigurer,
            CaptainApiProperties config,
            ObjectProvider<ReactiveOAuth2AuthorizedClientManager> oAuth2AuthorizedClientManager
    ) {
        return new CaptainResourceLinkageLookupGateway(webclientBuilder, CaptainApiConfig.builder()
                .url(config.getUrl())
                .bearer(config.getBearer())
                .authorizedClientManager(oAuth2AuthorizedClientManager.getIfAvailable())
                .build(), webClientConfigurer);
    }

    @Data
    private class CaptainApiProperties {
        String url;
        String bearer;
    }
}
