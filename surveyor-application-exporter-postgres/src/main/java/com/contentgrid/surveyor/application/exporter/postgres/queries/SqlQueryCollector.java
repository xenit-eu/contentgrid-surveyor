package com.contentgrid.surveyor.application.exporter.postgres.queries;

import com.contentgrid.surveyor.application.exporter.postgres.connections.DatabaseConnectionManager;
import io.prometheus.metrics.model.registry.MultiCollector;
import io.prometheus.metrics.model.snapshots.GaugeSnapshot;
import io.prometheus.metrics.model.snapshots.GaugeSnapshot.GaugeDataPointSnapshot;
import io.prometheus.metrics.model.snapshots.MetricMetadata;
import io.prometheus.metrics.model.snapshots.MetricSnapshot;
import io.prometheus.metrics.model.snapshots.MetricSnapshots;
import java.sql.SQLException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

@RequiredArgsConstructor
@Slf4j
public class SqlQueryCollector implements MultiCollector {
    private final DatabaseConnectionManager connectionManager;
    private final List<SqlQueryExecutor> executors;
    private final Scheduler scheduler = Schedulers.newBoundedElastic(10, Integer.MAX_VALUE, "SqlQueryCollector");

    @Override
    public MetricSnapshots collect() {
        return Flux.fromStream(connectionManager.connections())
                .flatMap(connection -> Mono.fromCallable(() -> connection.lease(dbConn -> {
                                    return executors.stream()
                                            .flatMap(executor -> {
                                                try {
                                                    return executor.collect(dbConn);
                                                } catch (SQLException e) {
                                                    throw new RuntimeException(e);
                                                }
                                            })
                                            .toList();
                                }))
                                .subscribeOn(scheduler)
                )
                .flatMap(Flux::fromIterable)
                .collectMultimap(snapshot -> snapshot.metadata().getName())
                .map(snapshots -> new MetricSnapshots(
                        snapshots.entrySet().stream()
                                .map((entry) -> new GaugeSnapshot(new MetricMetadata(entry.getKey()), entry.getValue().stream().map(snapshot -> new GaugeDataPointSnapshot(snapshot.value(), snapshot.labels(), null)).toList()))
                                .map(MetricSnapshot.class::cast)
                                .toList()
                ))
                .block();
    }

    @Override
    public List<String> getPrometheusNames() {
        return executors.stream()
                .flatMap(SqlQueryExecutor::getMetadata)
                .map(MetricMetadata::getPrometheusName)
                .toList();
    }
}
