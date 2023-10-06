package com.contentgrid.surveyor.spi.storage.aggregation;

import java.time.Duration;
import java.util.function.Function;
import lombok.NonNull;

public record BucketingOperation(
        @NonNull
        Duration bucket,
        @NonNull
        AggregationOperation operation
) implements Operation {

    @Override
    public <T> T perform(Function<BucketingOperation, T> bucketingOperation,
            Function<FinishingOperation, T> finishingOperation) {
        return bucketingOperation.apply(this);
    }
}
