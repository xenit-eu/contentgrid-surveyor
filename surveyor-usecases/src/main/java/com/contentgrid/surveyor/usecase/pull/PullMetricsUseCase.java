package com.contentgrid.surveyor.usecase.pull;

import com.contentgrid.surveyor.api.pull.PullMetrics;
import com.contentgrid.surveyor.spi.TimeInterval;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
        if (!Instant.now().isAfter(timeInterval.getEndTime())) {
            log.info("Not pulling new metrics from source {} with query '{}' as we are still within the interval {}",
                    metricSource, metricCollectionConfig.query(),
                    timeInterval);
            return true;
        }

        var metrics = metricSource.collectMetrics(metricCollectionConfig, timeInterval.getStartTime())
                .map(metric -> new EventCountMetric(
                        metric.timeInterval(),
                        new Resource(metricSource.resourceDefinition(metricCollectionConfig).orElseThrow(),
                                metric.resourceId()),
                        metric.value()
                )).toList();
        log.info("Pulled new metrics from source {} with query '{}' (interval {}): {} datapoints", metricSource,
                metricCollectionConfig.query(), timeInterval,
                metrics.size());

        storeEventCountMetricSpiPort.storeEventMetrics(metrics);

        return !metrics.isEmpty();
    }

    private boolean tryPullMetricsBulk(MetricCollectionConfig metricCollectionConfig, EventMetricsSource metricSource,
            TimeInterval timeInterval) throws CollectionFailedException {
        if (!Instant.now().isAfter(timeInterval.getEndTime())) {
            log.info("Not pulling new metrics from source {} with query '{}' as we are still within the interval {}",
                    metricSource, metricCollectionConfig.query(),
                    timeInterval);
            return true;
        }

        var metrics = metricSource.collectMetricsForBackfilling(metricCollectionConfig, timeInterval)
                .map(metric -> new EventCountMetric(
                        metric.timeInterval(),
                        new Resource(metricSource.resourceDefinition(metricCollectionConfig).orElseThrow(),
                                metric.resourceId()),
                        metric.value()
                )).toList();
        log.info("Pulled new metrics from source {} with query '{}' (interval {}): {} datapoints", metricSource,
                metricCollectionConfig.query(), timeInterval,
                metrics.size());

        storeEventCountMetricSpiPort.storeEventMetrics(metrics);

        return !metrics.isEmpty();
    }
}
