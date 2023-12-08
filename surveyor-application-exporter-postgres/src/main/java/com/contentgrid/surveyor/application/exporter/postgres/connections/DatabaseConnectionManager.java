package com.contentgrid.surveyor.application.exporter.postgres.connections;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.observation.ObservationRegistry;
import java.util.Base64;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.SmartLifecycle;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;

@RequiredArgsConstructor
@Slf4j
public class DatabaseConnectionManager implements SmartLifecycle, MeterBinder {

    public static final String DATASOURCE_URL = "spring.datasource.url";
    public static final String DATASOURCE_USERNAME = "spring.datasource.username";
    public static final String DATASOURCE_PASSWORD = "spring.datasource.password";

    private final SharedIndexInformer<Secret> informer;
    private final ObservationRegistry observationRegistry;

    @Override
    public void bindTo(MeterRegistry registry) {
        registry.gauge("databases.discovered", informer, i -> connections().count());
    }

    public Stream<DatabaseConnection> connections() {
        return informer.getStore().list()
                .stream()
                .filter(secret -> secret.getData().containsKey(DATASOURCE_URL))
                .map(secret -> new DatabaseConnection(
                        decodeSecret(secret, DATASOURCE_URL),
                        observationRegistry,
                        DataSourceBuilder.create()
                                .type(SimpleDriverDataSource.class)
                                .url(decodeSecret(secret, DATASOURCE_URL))
                                .username(decodeSecret(secret, DATASOURCE_USERNAME))
                                .password(decodeSecret(secret, DATASOURCE_PASSWORD))
                                .build()
                ));
    }

    private static String decodeSecret(Secret secret, String key) {
        var encoded = secret.getData().get(key);
        if (encoded == null) {
            return null;
        }
        return new String(Base64.getDecoder().decode(encoded));
    }

    @Override
    public void start() {
        informer.run();
        log.info("Started informer");
    }

    @Override
    public void stop() {
        informer.stop();
        log.info("Terminated informer");
    }

    @Override
    public boolean isRunning() {
        return informer.isRunning();
    }

}
