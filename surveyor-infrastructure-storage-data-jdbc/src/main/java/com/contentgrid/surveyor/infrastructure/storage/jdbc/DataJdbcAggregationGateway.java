package com.contentgrid.surveyor.infrastructure.storage.jdbc;


import com.contentgrid.surveyor.spi.TimeInterval;
import com.contentgrid.surveyor.spi.storage.AggregateEventCountMetricSpiPort;
import com.contentgrid.surveyor.spi.storage.EventCountMetric;
import com.contentgrid.surveyor.spi.storage.Resource;
import com.contentgrid.surveyor.spi.storage.aggregation.AggregationConfiguration;
import com.contentgrid.surveyor.spi.storage.aggregation.AggregationOperation;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.ConversionService;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@RequiredArgsConstructor
public class DataJdbcAggregationGateway implements AggregateEventCountMetricSpiPort {

    private final ResourceRepository resourceRepository;
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ConversionService conversionService;

    @Override
    public List<EventCountMetric> findEventCountMetrics(Resource resource, TimeInterval interval,
            AggregationConfiguration aggregationConfiguration) {
        var resourceEntity = resourceRepository.upsert(ResourceEntity.from(resource));
        var query = buildQuery(aggregationConfiguration);

        return jdbcTemplate.query(
                query,
                Map.of(
                        "resourceId", resourceEntity.getId(),
                        "startTime", conversionService.convert(interval.getStartTime(), Timestamp.class),
                        "endTime", conversionService.convert(interval.getEndTime(), Timestamp.class)
                ),
                (rs, rowNum) -> new EventCountMetric(
                        TimeInterval.between(
                                conversionService.convert(rs.getTimestamp("start_time"), Instant.class),
                                conversionService.convert(rs.getTimestamp("end_time"), Instant.class)
                        ),
                        resource,
                        rs.getBigDecimal("value")
                )
        );
    }

    private String buildQuery(AggregationConfiguration aggregationConfiguration) {
        if (aggregationConfiguration.isEmpty()) {
            return """
                    select * from metric_events
                        where resource_id = :resourceId
                        and end_time > :startTime and end_time <= :endTime
                    """;
        }
        var split = aggregationConfiguration.splitRight();
        var operation = split.operation();

        var query = operation.perform(
                bucketingOperation -> {
                    // Note that we subtract 1 microsecond from the end time to ensure we don't get an additional interval
                    // where the end time aligns with the time bucket (end_time is the exclusive boundary)
                    return """
                            select time_bucket('%s', m.end_time - '1 microsecond'::interval, :startTime ::timestamptz),
                                resource_id,
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
                            select resource_id,
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

        return query.formatted(buildQuery(split));
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
