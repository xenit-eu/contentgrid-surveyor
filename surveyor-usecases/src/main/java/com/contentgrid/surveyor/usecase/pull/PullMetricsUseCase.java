package com.contentgrid.surveyor.usecase.pull;

import com.contentgrid.surveyor.api.pull.PullMetrics;
import com.contentgrid.surveyor.spi.TimeInterval;
import com.contentgrid.surveyor.spi.config.FindCollectionConfigurationsSpiPort;
import com.contentgrid.surveyor.spi.collector.CollectedMeasurement;
import com.contentgrid.surveyor.spi.collector.MeasurementCollector;
import com.contentgrid.surveyor.spi.config.MetricCollectionConfig;
import com.contentgrid.surveyor.spi.storage.Measurement;
import com.contentgrid.surveyor.spi.storage.LastMeasurementSpiPort;
import com.contentgrid.surveyor.spi.storage.StoreMeasurementSpiPort;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@Slf4j
public class PullMetricsUseCase implements PullMetrics {

    private final List<? extends MeasurementCollector> measurementCollectors;
    private final FindCollectionConfigurationsSpiPort findCollectionConfigurationsSpiPort;
    private final StoreMeasurementSpiPort storeMeasurementSpiPort;
    private final LastMeasurementSpiPort lastMeasurementSpiPort;

    private boolean isLastPullSuccesfull = false;

    @Override
    public void pullMetrics() {
        for (MeasurementCollector measurementCollector : measurementCollectors) {
            var configs = findCollectionConfigurationsSpiPort.findConfigurationsFor(
                    measurementCollector.getSystemType());
            for (var collectionConfig : configs) {
                var maybeResourceDefinition = measurementCollector.resourceDefinition(collectionConfig);
                if (maybeResourceDefinition.isEmpty()) {
                    continue;
                }
                var resourceDefinition = maybeResourceDefinition.orElseThrow();
                var maybeLastEvent = lastMeasurementSpiPort.getLastMeasurementInterval(
                        resourceDefinition);

                var referenceTime = Instant.now().truncatedTo(ChronoUnit.DAYS);
                maybeLastEvent
                        .map(TimeInterval::nextInterval)
                        .switchIfEmpty(Mono.fromSupplier(() -> {
                            var referenceInterval = TimeInterval.between(referenceTime.minus(Duration.ofDays(30)),
                                    referenceTime);
                            log.warn(
                                    "Metrics for {} are not initialized. Pulling data in bulk for interval {}",
                                    resourceDefinition, referenceInterval);
                            return referenceInterval;
                        }))
                        .flatMap(nextInterval -> {
                            var deltaFromReference = Duration.between(nextInterval.getStartTime(), referenceTime);

                            if (deltaFromReference.dividedBy(collectionConfig.interval()) > 3 || !Objects.equals(
                                    nextInterval.getDuration(), collectionConfig.interval())) {
                                log.warn("Running behind a lot when fetching {}: delta is {}; bulk-fetching",
                                        resourceDefinition,
                                        deltaFromReference);
                                nextInterval = TimeInterval.between(nextInterval.getStartTime(), referenceTime)
                                        .alignedToMultipleOf(collectionConfig.interval());
                                return Flux.fromStream(nextInterval.chunkedBy(Duration.ofDays(1)))
                                        .concatMap(
                                                interval -> tryPullMetricsBulk(collectionConfig, measurementCollector,
                                                        interval))
                                        .then();
                            }

                            return Mono.just(nextInterval).expand(interval -> {
                                return tryPullMetrics(collectionConfig, measurementCollector, interval)
                                        .flatMap(result -> result.continueLoop ? Mono.just(interval.nextInterval())
                                                : Mono.empty());
                            }).then();
                        })
                        .doOnSuccess(m -> this.isLastPullSuccesfull = true)
                        .doOnError(error -> {
                            log.error("Failed to pull metrics for {}", resourceDefinition, error);
                            this.isLastPullSuccesfull = false;
                        })
                        .onErrorComplete()
                        .block();
            }

        }
    }

    public boolean isLastPullSuccesfull() {
        return this.isLastPullSuccesfull;
    }


    private Mono<PullResult> tryPullMetrics(MetricCollectionConfig metricCollectionConfig,
            MeasurementCollector measurementCollector,
            TimeInterval timeInterval) {
        return tryPullMetricsGeneric(measurementCollector, metricCollectionConfig, timeInterval,
                (source, config, interval) -> source.collectMeasurements(config, interval.getStartTime()));
    }

    private Mono<PullResult> tryPullMetricsBulk(MetricCollectionConfig metricCollectionConfig,
            MeasurementCollector measurementCollector,
            TimeInterval timeInterval) {
        return tryPullMetricsGeneric(measurementCollector, metricCollectionConfig, timeInterval,
                MeasurementCollector::collectMeasurementsForBackfilling);
    }

    @FunctionalInterface
    interface MetricCollector {

        Publisher<CollectedMeasurement> collect(MeasurementCollector measurementCollector,
                MetricCollectionConfig config,
                TimeInterval timeInterval);
    }

    private Mono<PullResult> tryPullMetricsGeneric(MeasurementCollector measurementCollector,
            MetricCollectionConfig metricCollectionConfig, TimeInterval timeInterval,
            MetricCollector metricCollector) {
        if (!Instant.now().isAfter(timeInterval.getEndTime())) {
            log.debug("Not pulling new metrics from source {} with query '{}' as we are still within the interval {}",
                    measurementCollector, metricCollectionConfig.query(),
                    timeInterval);
            return Mono.just(PullResult.TOO_SOON);
        }

        var resourceDefinition = measurementCollector.resourceDefinition(metricCollectionConfig).orElseThrow();

        var metrics = Flux.from(metricCollector.collect(measurementCollector, metricCollectionConfig, timeInterval))
                .checkpoint(
                        "Metrics pull from source %s with query '%s' over interval '%s' [PullMetricsUseCase]".formatted(
                                measurementCollector,
                                metricCollectionConfig.query(), timeInterval))
                .map(metric -> new Measurement(
                        metric.timeInterval(),
                        resourceDefinition.createMetric(metric.resourceId(), Map.of()),
                        metric.value()
                ));

        AtomicLong metricSize = new AtomicLong();
        return metrics.doOnNext(item -> metricSize.getAndIncrement())
                .doOnComplete(() -> {
                    if (metricSize.get() == 0) {
                        log.warn("Failed to pull metrics for {}: no data in interval {}", resourceDefinition,
                                timeInterval);
                    } else {
                        log.info("Pulled new metrics from source {} with query '{}' (interval {}): {} datapoints",
                                measurementCollector,
                                metricCollectionConfig.query(), timeInterval,
                                metricSize.get());
                    }
                })
                .buffer(1000)
                .flatMap(storeMeasurementSpiPort::storeMeasurements)
                .count()
                .map(count -> count > 0 ? PullResult.RECEIVED_DATA : PullResult.NO_DATA);
    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    enum PullResult {
        TOO_SOON(false, false),
        NO_DATA(true, false),
        RECEIVED_DATA(true, true);

        private final boolean continueLoop;
        private final boolean hasData;

    }
}
