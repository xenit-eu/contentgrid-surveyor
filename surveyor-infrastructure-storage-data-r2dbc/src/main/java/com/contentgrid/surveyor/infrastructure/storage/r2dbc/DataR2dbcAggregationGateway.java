package com.contentgrid.surveyor.infrastructure.storage.r2dbc;


import com.contentgrid.surveyor.infrastructure.storage.r2dbc.MetricRepository.MetricAndResourceIdentityView;
import com.contentgrid.surveyor.spi.ResourceDefinition;
import com.contentgrid.surveyor.spi.TimeInterval;
import com.contentgrid.surveyor.spi.resources.Metric;
import com.contentgrid.surveyor.spi.storage.AggregateMeasurementsSpiPort;
import com.contentgrid.surveyor.spi.storage.Measurement;
import com.contentgrid.surveyor.spi.storage.aggregation.AggregationConfiguration;
import com.contentgrid.surveyor.spi.storage.aggregation.AggregationOperation;
import io.r2dbc.spi.Readable;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;

@RequiredArgsConstructor
public class DataR2dbcAggregationGateway implements AggregateMeasurementsSpiPort {

    private final MetricRepository metricRepository;
    private final DatabaseClient databaseClient;

    @Override
    public Flux<Measurement> findMeasurements(Metric resource, TimeInterval interval,
            AggregationConfiguration aggregationConfiguration) {
        var query = databaseClient.sql(() -> buildQuery(aggregationConfiguration, """
                select * from measurement
                    where metric_id = :metricId
                    and end_time > :startTime and end_time <= :endTime
                """));
        return metricRepository.find(resource)
                .flatMapMany(metricEntity -> {
                    return query.bind("metricId", metricEntity.id())
                            .bind("startTime", interval.getStartTime())
                            .bind("endTime", interval.getEndTime())
                            .map((rs) -> createMeasurement(resource, rs))
                            .all();
                });
    }

    private Measurement createMeasurement(Metric resource, Readable rs) {
        return new Measurement(
                TimeInterval.between(
                        rs.get("start_time", Instant.class),
                        rs.get("end_time", Instant.class)
                ),
                resource,
                rs.get("value", BigDecimal.class)
        );
    }

    @Override
    @Transactional
    public Flux<Measurement> findMeasurements(ResourceDefinition resourceDefinition,
            TimeInterval interval,
            AggregationConfiguration aggregationConfiguration) {
        var query = databaseClient.sql(() -> buildQuery(aggregationConfiguration, """
                        select * from measurement m
                            where metric_id IN(:metricIds)
                            and end_time > :startTime and end_time <= :endTime
                        """))
                .bind("startTime", interval.getStartTime())
                .bind("endTime", interval.getEndTime());
        return metricRepository.findAllByResourceDefinition(resourceDefinition)
                .buffer(100)
                .map(metricEntities -> metricEntities.stream()
                        .collect(Collectors.toMap(MetricAndResourceIdentityView::id, Function.identity())))
                .flatMap(metricEntities -> {
                    return query
                            .bind("metricIds", metricEntities.keySet())
                            .map((rs) -> {
                                var metricEntity = metricEntities.get(rs.get("metric_id", Long.class));
                                var metric = metricEntity.toDomain();
                                return createMeasurement(metric, rs);
                            })
                            .all();
                });
    }

    private String buildQuery(AggregationConfiguration aggregationConfiguration, String baseCase) {
        if (aggregationConfiguration.isEmpty()) {
            return baseCase;
        }
        var split = aggregationConfiguration.splitRight();
        var operation = split.operation();

        var query = operation.perform(
                bucketingOperation -> {
                    // Note that we subtract 1 microsecond from the end time to ensure we don't get an additional interval
                    // where the end time aligns with the time bucket (end_time is the exclusive boundary)
                    return """
                            select time_bucket('%s', m.end_time - '1 microsecond'::interval, :startTime ::timestamptz),
                                metric_id,
                                min(m.start_time) as start_time,
                                max(m.end_time) as end_time,
                                %s(m.value) as value
                                from (%%s) m
                                group by 1, 2
                            """.formatted(
                            toPostgresInterval(bucketingOperation.bucket()),
                            toPostgresFunction(bucketingOperation.operation())
                    );

                },
                finishingOperation -> {
                    return """
                            select metric_id,
                                min(m.start_time) as start_time,
                                max(m.end_time) as end_time,
                                %s(m.value) as value
                                from (%%s) m
                                group by 1
                            """.formatted(
                            toPostgresFunction(finishingOperation.operation())
                    );
                }
        );

        return query.formatted(buildQuery(split, baseCase));
    }

    private String toPostgresInterval(Duration groupInterval) {
        return groupInterval.truncatedTo(ChronoUnit.SECONDS).toString();
    }

    private String toPostgresFunction(AggregationOperation operation) {
        return switch (operation) {
            case AVERAGE -> "avg";
            case SUM -> "sum";
            case MAX -> "max";
            case MIN -> "min";
        };
    }

}
