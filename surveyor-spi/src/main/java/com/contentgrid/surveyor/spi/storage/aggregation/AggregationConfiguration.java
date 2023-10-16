package com.contentgrid.surveyor.spi.storage.aggregation;

import java.time.Duration;

public interface AggregationConfiguration {

    boolean isEmpty();

    AggregationConfigurationSplit splitLeft();

    AggregationConfigurationSplit splitRight();

    static AggregationConfigurationBuilder builder() {
        return new AggregationConfigurationOperations();

    }

    interface AggregationConfigurationBuilder {

        AggregationConfigurationBuilder thenBucket(Duration bucketSize, AggregationOperation operation);

        AggregationConfiguration finallyAggregate(AggregationOperation operation);

        AggregationConfiguration finallyDontAggregate();
    }
}
