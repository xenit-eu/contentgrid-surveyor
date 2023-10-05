package com.contentgrid.surveyor.spi.storage;

import com.contentgrid.surveyor.spi.TimeInterval;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Singular;

public interface AggregateEventCountMetricSpiPort {

    default EventCountMetric getAggregatedEventCountMetric(Resource resource, TimeInterval interval,
            AggregationConfiguration aggregationConfiguration) {
        return Util.onlyValue(
                findEventCountMetrics(resource, interval, aggregationConfiguration),
                () -> new EventCountMetric(interval, resource, BigDecimal.ZERO)
        );
    }

    List<EventCountMetric> findEventCountMetrics(Resource resource, TimeInterval interval,
            AggregationConfiguration aggregationConfiguration);

    @Builder
    class AggregationConfiguration {

        @Singular
        @NonNull
        @Getter
        private final List<GroupingConfiguration> groupings;

        public boolean isEmpty() {
            return groupings.isEmpty();
        }

        public enum GroupOperation {
            AVERAGE,
            MAX,
            MIN,
            SUM
        }

        public static class AggregationConfigurationBuilder {

            public AggregationConfigurationBuilder thenGroup(GroupOperation operation, Duration groupInterval) {
                return grouping(new GroupingConfiguration(groupInterval, operation));
            }
        }

        public record GroupingConfiguration(
                @NonNull
                Duration groupInterval,
                @NonNull
                GroupOperation operation
        ) {

        }

    }


}
