package com.contentgrid.surveyor.spi.storage.aggregation;

import com.contentgrid.surveyor.spi.storage.aggregation.AggregationConfiguration.AggregationConfigurationBuilder;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class AggregationConfigurationOperations implements AggregationConfiguration, AggregationConfigurationBuilder {

    @NonNull
    private final List<Operation> operations;

    AggregationConfigurationOperations() {
        this(List.of());
    }

    @Override
    public boolean isEmpty() {
        return operations.isEmpty();
    }

    @Override
    public AggregationConfigurationSplit splitLeft() {
        return new AggregationConfigurationSplit(
                operations.get(0),
                new AggregationConfigurationOperations(operations.subList(1, operations.size()))
        );
    }

    @Override
    public AggregationConfigurationSplit splitRight() {
        int lastItem = operations.size() - 1;
        return new AggregationConfigurationSplit(
                operations.get(lastItem),
                new AggregationConfigurationOperations(operations.subList(0, lastItem))
        );
    }

    @Override
    public AggregationConfigurationBuilder thenBucket(Duration bucket, AggregationOperation aggregationOperation) {
        return withAppendedOperation(new BucketingOperation(bucket, aggregationOperation));
    }

    @Override
    public AggregationConfiguration finallyAggregate(AggregationOperation operation) {
        return withAppendedOperation(new FinishingOperation(operation));
    }

    private AggregationConfigurationOperations withAppendedOperation(Operation operation) {
        var ops = new ArrayList<>(operations);
        ops.add(operation);
        return new AggregationConfigurationOperations(ops);
    }
}
