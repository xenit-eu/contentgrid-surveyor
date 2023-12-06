package com.contentgrid.surveyor.application.exporter.postgres.queries;

import io.prometheus.metrics.model.snapshots.GaugeSnapshot;
import io.prometheus.metrics.model.snapshots.GaugeSnapshot.Builder;
import io.prometheus.metrics.model.snapshots.GaugeSnapshot.GaugeDataPointSnapshot;
import io.prometheus.metrics.model.snapshots.Labels;
import io.prometheus.metrics.model.snapshots.MetricMetadata;
import io.prometheus.metrics.model.snapshots.MetricSnapshot;
import io.prometheus.metrics.model.snapshots.MetricSnapshots;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class SqlQueryExecutor {
    private final QueryMetricProperties properties;

    public Stream<MetricMetadata> getMetadata() {
        return createMetadata().values().stream();
    }

    private Map<String, MetricMetadata> createMetadata() {
        return properties.getValues()
                .stream()
                .map(valueColumnName -> Map.entry(valueColumnName, new MetricMetadata(properties.getMetricName()+"_"+valueColumnName)))
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue));

    }

    public Stream<DatapointSnapshot> collect(Connection databaseConnection) throws SQLException {
        log.info("Collecting query '{}'", properties.getQuery());
        var metricMetadata = createMetadata();

        var streamBuilder = Stream.<List<DatapointSnapshot>>builder();
        AtomicLong count = new AtomicLong();
        try(var statement = databaseConnection.createStatement()) {
            try(var resultSet = statement.executeQuery(properties.getQuery())) {
                while(resultSet.next()) {
                    count.incrementAndGet();
                    streamBuilder.add(handleRow(metricMetadata, resultSet));
                }
            }
        }

        log.info("Collected query '{}' into {} rows ({} datapoints each)", properties.getQuery(), count.get(), metricMetadata.size());

        return streamBuilder.build()
                .flatMap(Collection::stream);
    }

    private List<DatapointSnapshot> handleRow(Map<String, MetricMetadata> metadata, ResultSet resultSet) {
        var labels = Labels.of(properties.getKeyLabels(), properties.getKeyLabels()
                .stream()
                .map(labelColumnName -> {
                    try {
                        return resultSet.getString(labelColumnName);
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                })
                .toList()
        );
        return properties.getValues().stream()
                .map(valueColumnName -> {
                    try {
                        var value = resultSet.getDouble(valueColumnName);
                        return new DatapointSnapshot(
                                metadata.get(valueColumnName),
                                labels,
                                value
                        );
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                })
                .toList();
    }

    public record DatapointSnapshot(MetricMetadata metadata, Labels labels, double value) {

    }

}
