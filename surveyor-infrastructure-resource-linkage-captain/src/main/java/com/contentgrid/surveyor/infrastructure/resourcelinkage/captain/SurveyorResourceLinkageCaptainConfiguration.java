package com.contentgrid.surveyor.infrastructure.resourcelinkage.captain;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.hateoas.config.HypermediaWebClientConfigurer;
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
            CaptainApiProperties config
    ) {
        return new CaptainResourceLinkageLookupGateway(webclientBuilder, CaptainApiConfig.builder()
                .url(config.getUrl())
                .build(), webClientConfigurer);
    }

    @Data
    private class CaptainApiProperties {
        String url;

    }
}
