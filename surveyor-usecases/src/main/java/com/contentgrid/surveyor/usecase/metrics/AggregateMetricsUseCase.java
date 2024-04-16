package com.contentgrid.surveyor.usecase.metrics;

import com.contentgrid.surveyor.api.metrics.AggregateBillingMetrics;
import com.contentgrid.surveyor.api.metrics.ExportedMetrics;
import com.contentgrid.surveyor.api.metrics.Metric;
import com.contentgrid.surveyor.api.metrics.Resource;
import com.contentgrid.surveyor.api.metrics.ResourceMetric;
import com.contentgrid.surveyor.spi.ResourceDefinition;
import com.contentgrid.surveyor.spi.TimeInterval;
import com.contentgrid.surveyor.spi.config.FindMeasurementAggregationConfigurationSpiPort;
import com.contentgrid.surveyor.spi.config.FindResourceDefinitionsSpiPort;
import com.contentgrid.surveyor.spi.resources.LinkedMeasurements;
import com.contentgrid.surveyor.spi.resources.LookupResourceLinkSpiPort;
import com.contentgrid.surveyor.spi.resources.ResourceLinkage;
import com.contentgrid.surveyor.spi.storage.AggregateMeasurementsSpiPort;
import com.contentgrid.surveyor.spi.storage.Measurement;
import com.contentgrid.surveyor.spi.storage.aggregation.AggregationConfiguration;
import com.contentgrid.surveyor.values.MetricName;
import com.contentgrid.surveyor.values.ResourceAndMetric;
import com.contentgrid.surveyor.values.ResourceType;
import com.contentgrid.surveyor.values.SourceName;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class AggregateMetricsUseCase implements AggregateBillingMetrics {

    private final FindMeasurementAggregationConfigurationSpiPort findMeasurementAggregationConfigurationSpiPort;
    private final AggregateMeasurementsSpiPort aggregateMeasurementsSpiPort;
    private final FindResourceDefinitionsSpiPort findResourceDefinitionsSpiPort;
    private final LookupResourceLinkSpiPort lookupResourceLinkSpiPort;

    record LinkedMeasurement(Measurement measurement, ResourceLinkage linkage){}

    @Override
    public Flux<LinkedMeasurements> findMetricsForBilling(AggregateBillingMetricsCommand command) {
        var interval = TimeInterval.between(command.start(), command.end());
        var resourceDefinitions = findResourceDefinitionsSpiPort.findResourceDefinitions(List.of(
                ResourceAndMetric.of("storage", "objects_count"),
                ResourceAndMetric.of("storage", "stored_bytes"),
                ResourceAndMetric.of("api", "request_count"),
                ResourceAndMetric.of("db", "estimated_count")));

        Function<ResourceDefinition, AggregationConfiguration> getConfig = (def) ->
                findMeasurementAggregationConfigurationSpiPort.getBillingAggregationConfiguration(def, interval.getDuration());

        var measurements = Flux.fromIterable(resourceDefinitions)
                .flatMap(def -> aggregateMeasurementsSpiPort.findMeasurements(def, interval, getConfig.apply(def))).log();

        var linkedMeasurements = measurements.flatMap(measure ->
                getLink(measure)
                        .map(a -> new LinkedMeasurement(measure, a))
                        .switchIfEmpty(Mono.just(new LinkedMeasurement(measure, null)))
        );

        var groupedMeasurements = linkedMeasurements.groupBy(LinkedMeasurement::linkage)
                .flatMap(groupedFlux -> groupedFlux
                        .map(x -> x.measurement())
                        .collectList()
                        .map(x -> new LinkedMeasurements(measurementsToMap(x), groupedFlux.key())));

        return groupedMeasurements;
    }

    private Mono<ResourceLinkage> getLink(Measurement measurement) {
        return lookupResourceLinkSpiPort.lookupLinkageForResource(measurement.getMetric().getResourceIdentity());
    }

    private static Map<MetricName, Measurement> measurementsToMap(List<Measurement> measurements) {
        Map<MetricName, Measurement> map = new HashMap<>();
        for (Measurement m : measurements) {
            map.put(m.getMetric().getMetricName(), m);
        }
        return map;
    }

}
