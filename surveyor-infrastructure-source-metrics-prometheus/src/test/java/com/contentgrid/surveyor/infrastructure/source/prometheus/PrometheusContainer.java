package com.contentgrid.surveyor.infrastructure.source.prometheus;

import java.net.URI;
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
}
