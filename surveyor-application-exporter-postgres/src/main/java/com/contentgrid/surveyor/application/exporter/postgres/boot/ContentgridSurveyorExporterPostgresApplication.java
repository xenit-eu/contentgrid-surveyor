package com.contentgrid.surveyor.application.exporter.postgres.boot;

import com.contentgrid.surveyor.application.exporter.postgres.connections.DatabaseConnectionManager;
import com.contentgrid.surveyor.application.exporter.postgres.queries.SqlQueryCollector;
import com.contentgrid.surveyor.application.exporter.postgres.queries.SqlQueryExecutor;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.micrometer.observation.ObservationRegistry;
import io.prometheus.metrics.exporter.common.PrometheusScrapeHandler;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@Slf4j
@EnableConfigurationProperties(SurveyorExporterProperties.class)
public class ContentgridSurveyorExporterPostgresApplication {

    public static void main(String[] args) {
        SpringApplication.run(ContentgridSurveyorExporterPostgresApplication.class, args);
    }

    @Bean
    KubernetesClient kubernetesClient(SurveyorExporterProperties properties) {
        var config = properties.getKubernetes();
        if (!properties.isKubernetesConfigured()) {
            log.warn("Using autoconfiguration for kubernetes client");
            config = Config.autoConfigure(null);
        }
        // If you pass namespace via surveyor.exporter.kubernetes.namespace, the configuration of the
        // kubernetes client will not be autoconfigured based on the kubeconfig file. You need to pass
        // everything to surveyor.exporter.kubernetes instead.
        // If you pass namespace via surveyor.exporter.user-apps-namespace, the configuration of the
        // kubernetes client will be autoconfigured first (assuming you did not provide any properties
        // under surveyor.exporter.kubernetes), and then the namespace of the configuration gets
        // overridden with the value of surveyor.exporter.user-apps-namespace.
        if (properties.getUserAppsNamespace() != null) {
            config.setNamespace(properties.getUserAppsNamespace());
        }
        return new KubernetesClientBuilder().withConfig(config).build();
    }

    @Bean
    DatabaseConnectionManager databaseConnectionManager(
            KubernetesClient kubernetesClient,
            SurveyorExporterProperties exporterProperties,
            ObservationRegistry observationRegistry
    ) {
        var informer = kubernetesClient.secrets()
                .withLabels(exporterProperties.getDiscovery().getMatchLabels())
                .runnableInformer(exporterProperties.getDiscovery().getResync().toMillis());

        return new DatabaseConnectionManager(informer, observationRegistry);
    }

    @Bean
    SqlQueryCollector sqlQueryCollector(DatabaseConnectionManager connectionManager, SurveyorExporterProperties exporterProperties) {
        return new SqlQueryCollector(
                connectionManager,
                exporterProperties.getMetrics()
                        .stream()
                        .map(SqlQueryExecutor::new)
                        .toList()
        );
    }

    @Bean
    MetricsController metricsController(SqlQueryCollector sqlQueryCollector, Executor executor) {
        // Don't make a bean out of this registry, or you get a dependency loop
        var registry = new PrometheusRegistry();
        registry.register(sqlQueryCollector);

        return new MetricsController(new PrometheusScrapeHandler(registry), executor);
    }
}
