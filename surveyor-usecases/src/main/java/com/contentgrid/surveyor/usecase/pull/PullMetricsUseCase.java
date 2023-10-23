package com.contentgrid.surveyor.usecase.pull;

import com.contentgrid.surveyor.api.pull.PullMetrics;
import com.contentgrid.surveyor.spi.TimeInterval;
import com.contentgrid.surveyor.spi.source.CollectedMetric;
import com.contentgrid.surveyor.spi.source.EventMetricsSource;
import com.contentgrid.surveyor.spi.source.EventMetricsSource.CollectionFailedException;
import com.contentgrid.surveyor.spi.source.MetricCollectionConfig;
import com.contentgrid.surveyor.spi.storage.EventCountMetric;
import com.contentgrid.surveyor.spi.storage.LastEventCountMetricSpiPort;
import com.contentgrid.surveyor.spi.storage.Resource;
import com.contentgrid.surveyor.spi.storage.StoreEventCountMetricSpiPort;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

@RequiredArgsConstructor
@Slf4j
public class PullMetricsUseCase implements PullMetrics {

    private final List<? extends EventMetricsSource> metricSources;
    private final List<MetricCollectionConfig> collectionConfigs;
    private final StoreEventCountMetricSpiPort storeEventCountMetricSpiPort;
    private final LastEventCountMetricSpiPort lastEventCountMetricSpiPort;

    @Override
    public void pullMetrics() {
        for (MetricCollectionConfig collectionConfig : collectionConfigs) {
            for (EventMetricsSource metricSource : metricSources) {
                var maybeResourceDefinition = metricSource.resourceDefinition(collectionConfig);
                if (maybeResourceDefinition.isEmpty()) {
                    continue;
                }
                var resourceDefinition = maybeResourceDefinition.orElseThrow();
                var maybeLastEvent = lastEventCountMetricSpiPort.getLastEventCountMetricInterval(resourceDefinition);

                var referenceTime = Instant.now().truncatedTo(ChronoUnit.DAYS);
                maybeLastEvent.ifPresentOrElse(lastEvent -> {
                    try {
                        var nextInterval = lastEvent.nextInterval();
                        var deltaFromReference = Duration.between(nextInterval.getEndTime(), referenceTime);

                        if (deltaFromReference.dividedBy(nextInterval.getDuration()) > 3) {
                            log.warn("Running behind a lot when fetching {}: delta is {}; bulk-fetching",
                                    resourceDefinition,
                                    deltaFromReference);
                            nextInterval = TimeInterval.between(nextInterval.getStartTime(), referenceTime)
                                    .alignedToMultipleOf(nextInterval.getDuration());
                            nextInterval.chunkedBy(Duration.ofDays(1)).forEachOrdered(interval -> {
                                try {
                                    tryPullMetricsBulk(collectionConfig, metricSource, interval);
                                    return;
                                } catch (CollectionFailedException e) {
                                    log.warn("Failed to pull metrics for {}", resourceDefinition, e);
                                }
                            });
                        }

                        boolean result;
                        do {
                            result = tryPullMetrics(collectionConfig, metricSource, nextInterval);
                            if (!result) {
                                log.warn(
                                        "Failed to pull metrics for {}: no data in interval {}. Skipping.",
                                        resourceDefinition, nextInterval);
                                nextInterval = nextInterval.nextInterval();
                            }
                        } while (!result);
                    } catch (CollectionFailedException e) {
                        log.error("Failed to pull new metrics for {}", resourceDefinition, e);
                    }
                }, () -> {
                    try {
                        var referenceInterval = TimeInterval.before(referenceTime.minus(Duration.ofDays(30)),
                                Duration.ofDays(1));
                        log.warn(
                                "Metrics for {} are not initialized. Pulling data in bulk for interval {}",
                                resourceDefinition, referenceInterval);
                        boolean result;
                        do {
                            result = tryPullMetricsBulk(collectionConfig, metricSource, referenceInterval);
                            if (!result) {
                                log.warn(
                                        "Failed to pull metrics for {}: no data in interval {}. Skipping.",
                                        resourceDefinition, referenceInterval.nextInterval());
                                referenceInterval = referenceInterval.nextInterval();
                            }
                        } while (!result);
                    } catch (CollectionFailedException e) {
                        log.error("Failed to pull new metrics for {}", resourceDefinition, e);
                    }
                });

            }

        }
    }


    private boolean tryPullMetrics(MetricCollectionConfig metricCollectionConfig, EventMetricsSource metricSource,
            TimeInterval timeInterval) throws CollectionFailedException {
        return tryPullMetricsGeneric(metricSource, metricCollectionConfig, timeInterval,
                (source, config, interval) -> source.collectMetrics(config, interval.getStartTime()));
    }

    private boolean tryPullMetricsBulk(MetricCollectionConfig metricCollectionConfig, EventMetricsSource metricSource,
            TimeInterval timeInterval) throws CollectionFailedException {
        return tryPullMetricsGeneric(metricSource, metricCollectionConfig, timeInterval,
                EventMetricsSource::collectMetricsForBackfilling);
    }

    @FunctionalInterface
    interface MetricCollector {

        Publisher<CollectedMetric> collect(EventMetricsSource metricsSource, MetricCollectionConfig config,
                TimeInterval timeInterval) throws CollectionFailedException;
    }

    private boolean tryPullMetricsGeneric(EventMetricsSource metricSource,
            MetricCollectionConfig metricCollectionConfig, TimeInterval timeInterval, MetricCollector metricCollector)
            throws CollectionFailedException {
        if (!Instant.now().isAfter(timeInterval.getEndTime())) {
            log.info("Not pulling new metrics from source {} with query '{}' as we are still within the interval {}",
                    metricSource, metricCollectionConfig.query(),
                    timeInterval);
            return true;
        }

        var metrics = Flux.from(metricCollector.collect(metricSource, metricCollectionConfig, timeInterval))
                .map(metric -> new EventCountMetric(
                        metric.timeInterval(),
                        new Resource(metricSource.resourceDefinition(metricCollectionConfig).orElseThrow(),
                                metric.resourceId()),
                        metric.value()
                ));

        AtomicLong metricSize = new AtomicLong();
        metrics.doOnNext(item -> metricSize.getAndIncrement())
                .doOnComplete(() -> {
                    log.info("Pulled new metrics from source {} with query '{}' (interval {}): {} datapoints",
                            metricSource,
                            metricCollectionConfig.query(), timeInterval,
                            metricSize.get());
                }).buffer(1000)
                .doOnNext(storeEventCountMetricSpiPort::storeEventMetrics)
                .blockLast();

        return metricSize.get() == 0;

    }
}
