package com.contentgrid.surveyor.application.exporter.postgres.boot;

import static org.assertj.core.api.Assertions.assertThat;

import com.contentgrid.surveyor.application.exporter.postgres.connections.DatabaseConnectionManager;
import com.contentgrid.surveyor.application.exporter.postgres.queries.SqlQueryCollector;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.prometheus.metrics.model.snapshots.DataPointSnapshot;
import io.prometheus.metrics.model.snapshots.Label;
import io.prometheus.metrics.model.snapshots.Labels;
import io.prometheus.metrics.model.snapshots.MetricSnapshot;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.k3s.K3sContainer;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@Testcontainers
class ContentgridSurveyorExporterPostgresApplicationTest {
    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer(DockerImageName.parse("postgres:latest"));
    @Container
    static K3sContainer kubernetes = new K3sContainer(DockerImageName.parse("rancher/k3s:latest"));

    @DynamicPropertySource
    static void kubernetesPropertySource(DynamicPropertyRegistry registry) {
        var config = Config.fromKubeconfig(kubernetes.getKubeConfigYaml());
        registry.add("surveyor.exporter.kubernetes.master-url", config::getMasterUrl);
        registry.add("surveyor.exporter.kubernetes.oauth-token", config::getOauthToken);
        registry.add("surveyor.exporter.kubernetes.username", config::getUsername);
        registry.add("surveyor.exporter.kubernetes.password", config::getPassword);
        registry.add("surveyor.exporter.kubernetes.namespace", config::getNamespace);
        registry.add("surveyor.exporter.kubernetes.ca-cert-data", config::getCaCertData);
        registry.add("surveyor.exporter.kubernetes.client-cert-data", config::getClientCertData);
        registry.add("surveyor.exporter.kubernetes.client-key-data", config::getClientKeyData);
        registry.add("surveyor.exporter.kubernetes.client-key-algo", config::getClientKeyAlgo);
        registry.add("surveyor.exporter.kubernetes.client-key-passphrase", config::getClientKeyPassphrase);
    }

    static DatabaseAccessCredentials db1;
    static DatabaseAccessCredentials db2;
    static DatabaseAccessCredentials db3;

    static Secret db1Secret;
    static Secret db2Secret;

    static KubernetesClient createKubernetesClient() {
        return new KubernetesClientBuilder()
                .withConfig(Config.fromKubeconfig(kubernetes.getKubeConfigYaml()))
                .build();
    }

    @BeforeAll
    static void createAndRegisterDatabases() throws SQLException {
        db1 = createDatabaseAndUser("database1");
        db2 = createDatabaseAndUser("database2");
        db3 = createDatabaseAndUser("database3");

        try(var kubernetesClient = createKubernetesClient()) {
            db1Secret = createSecret(kubernetesClient, db1);
            db2Secret = createSecret(kubernetesClient, db2);
        };

        try(var c1 = db1.createDataSource().getConnection()) {
            createTable(c1, "my_object", 50);
            createTable(c1, "other_object", 920);
            createTable(c1, "my_object__rel", 32);
        }

        try(var c2 = db2.createDataSource().getConnection()) {
            createTable(c2, "flyway_schema_history", 12);
            createTable(c2, "other_object", 20);
            createTable(c2, "other_object__self", 600);
        }

        try(var c3 = db3.createDataSource().getConnection()) {
            createTable(c3, "obj123", 20);
        }
    }

    record DatabaseAccessCredentials(
            String jdbcUri,
            String username,
            String password
    ) {
        public DataSource createDataSource() {
            return DataSourceBuilder.create()
                    .type(SimpleDriverDataSource.class)
                    .url(jdbcUri)
                    .username(username)
                    .password(password)
                    .build();
        }
    }

    private static DatabaseAccessCredentials createDatabaseAndUser(String dbName) throws SQLException {
        try(var connection = postgres.createConnection("")) {
            try(var statement = connection.createStatement()) {
                var username = dbName+"-user";
                var password = dbName+"-password";
                statement.addBatch("CREATE DATABASE \"%s\"".formatted(dbName));
                statement.addBatch("CREATE USER \"%s\" WITH PASSWORD '%s'".formatted(username, password));
                statement.addBatch("GRANT ALL ON DATABASE \"%s\" TO \"%s\"".formatted(dbName, username));
                statement.addBatch("ALTER DATABASE \"%s\" OWNER TO \"%s\"".formatted(dbName, username));
                statement.executeBatch();
                return new DatabaseAccessCredentials(
                        "jdbc:postgresql://" +
                                postgres.getHost() +
                                ":" +
                                postgres.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT) +
                                "/" +
                                dbName,
                        username,
                        password
                );
            }
        }
    }

    private static Secret createSecret(KubernetesClient client, DatabaseAccessCredentials credentials) {
        return client.secrets()
                .inNamespace("default")
                .resource(new SecretBuilder()
                        .withNewMetadata()
                        .withName(UUID.randomUUID()+"-db")
                        .addToLabels("app.contentgrid.com/service-type", "api")
                        .endMetadata()
                        .withStringData(Map.of(
                                "spring.datasource.url", credentials.jdbcUri(),
                                "spring.datasource.username", credentials.username(),
                                "spring.datasource.password", credentials.password()
                        ))
                        .build()
                )
                .create();
    }

    private static void createTable(Connection connection, String tableName, int entries) throws SQLException {
        try(var createTable = connection.createStatement()) {
            createTable.execute("CREATE TABLE \"%s\"(id bigint generated always as identity, v text)".formatted(tableName));
        }
        try(var inserts = connection.createStatement()) {
            for(int i = 0; i < entries; i++) {
                inserts.addBatch("INSERT INTO \"%s\"(v) VALUES('xxxx')".formatted(tableName));
            }
            inserts.addBatch("ANALYZE \"%s\"".formatted(tableName));
            inserts.executeBatch();
        }
    }

    @Autowired
    DatabaseConnectionManager connectionManager;

    @Autowired
    KubernetesClient kubernetesClient;

    @Autowired
    SqlQueryCollector sqlQueryCollector;

    @Test
    void discoversDatabases() throws InterruptedException {
        assertThat(connectionManager.connections()).hasSize(2);

        kubernetesClient.secrets()
                .resource(db1Secret)
                .delete();

        // Wait until the watcher has seen the change
        Thread.sleep(100);
        assertThat(connectionManager.connections()).hasSize(1);

        var secret3 = createSecret(kubernetesClient, db3);
        db1Secret = createSecret(kubernetesClient, db1);

        // Wait until the watcher has seen the change
        Thread.sleep(100);
        assertThat(connectionManager.connections()).hasSize(3);

        kubernetesClient.secrets()
                .resource(secret3)
                .delete();
    }

    @Test
    void createsMetricsForDatabases() {
        assertThat(sqlQueryCollector.collect())
                .map(snapshot -> snapshot.getMetadata().getName())
                .containsExactlyInAnyOrder(
                        "contentgrid_entity_database_disk_size_bytes",
                        "contentgrid_entity_database_estimated_count"
                );

        assertThat(sqlQueryCollector.collect())
                .flatMap(MetricSnapshot::getDataPoints)
                .allSatisfy(datapoint -> {
                    assertThat(datapoint.getLabels().stream())
                            .map(Label::getName)
                            .containsOnly("resource_id", "entity_name");

                })
                .map(DataPointSnapshot::getLabels)
                .containsAnyOf(
                        Labels.of("resource_id", "database1", "entity_name", "my_object"),
                        Labels.of("resource_id", "database1", "entity_name", "other_object"),
                        Labels.of("resource_id", "database2", "entity_name", "other_object")
                );

    }

}