package com.contentgrid.surveyor.infrastructure.source.prometheus.test;

import java.net.URI;
import org.springframework.web.reactive.function.client.WebClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

public class PrometheusContainer extends GenericContainer<PrometheusContainer> {

    public PrometheusContainer() {
        super(DockerImageName.parse("prom/prometheus"));
        addExposedPort(9090);
    }

    public URI getApiUrl() {
        return URI.create("http://" + getHost() + ":" + getMappedPort(9090) + "/");
    }

    public WebClient getClient() {
        return getClient(WebClient.builder());
    }

    public WebClient getClient(WebClient.Builder builder) {
        return builder
                .baseUrl(getApiUrl().toString())
                .build();
    }

}
