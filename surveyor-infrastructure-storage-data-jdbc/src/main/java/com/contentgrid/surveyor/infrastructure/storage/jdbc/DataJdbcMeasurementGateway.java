package com.contentgrid.surveyor.infrastructure.storage.jdbc;

import com.contentgrid.surveyor.infrastructure.storage.jdbc.MetricRepository.MetricAndResourceIdentityView;
import com.contentgrid.surveyor.spi.ResourceDefinition;
import com.contentgrid.surveyor.spi.TimeInterval;
import com.contentgrid.surveyor.spi.storage.Measurement;
import com.contentgrid.surveyor.spi.storage.LastMeasurementSpiPort;
import com.contentgrid.surveyor.spi.storage.StoreMeasurementSpiPort;
import java.util.List;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class DataJdbcMeasurementGateway implements StoreMeasurementSpiPort, LastMeasurementSpiPort {

    private final ResourceIdentityRepository resourceIdentityRepository;
    private final MetricRepository metricRepository;
    private final MeasurementRepository measurementRepository;

    @Override
    public Mono<TimeInterval> getLastMeasurementInterval(ResourceDefinition resourceDefinition) {
        return measurementRepository.findLast(resourceDefinition)
                .map(metric -> TimeInterval.between(metric.getStartTime(), metric.getEndTime()));
    }

    @Override
    public Mono<Void> storeMeasurements(List<Measurement> measurements) {
        var metricsMapMono = Flux.fromIterable(measurements)
                .map(Measurement::getMetric)
                .distinct()
                .flatMap(metric ->
                        // Attempt to look up metric
                        // TODO: lookup all metrics for measurements in bulk with one query
                        metricRepository.find(metric)
                                // Else maybe create resource entity and create metric
                                .switchIfEmpty(Mono.defer(() -> resourceIdentityRepository
                                        .findOrCreate(metric.getResourceIdentity())
                                        .flatMap(resourceIdentity -> metricRepository.upsert(
                                                        MetricEntity.from(resourceIdentity.getId(), metric))
                                                .map(metricEntity -> MetricAndResourceIdentityView.from(
                                                        resourceIdentity,
                                                        metricEntity))
                                        )
                                ))
                )
                .collectMap(MetricAndResourceIdentityView::toDomain, MetricAndResourceIdentityView::id);

        return metricsMapMono.flatMapMany(metricsMap -> {
            return Flux.fromIterable(measurements)
                    .map(measurement -> MeasurementEntity.builder()
                            .metricId(metricsMap.get(measurement.getMetric()))
                            .startTime(measurement.getMeasureInterval().getStartTime())
                            .endTime(measurement.getMeasureInterval().getEndTime())
                            .value(measurement.getValue())
                            .build()
                    )
                    .buffer(100)
                    .flatMap(measurementRepository::saveAll);
        }).then();
    }

    @Override
    public Mono<Void> storeMeasurement(Measurement measurement) {
        return storeMeasurements(List.of(measurement));
    }
}
