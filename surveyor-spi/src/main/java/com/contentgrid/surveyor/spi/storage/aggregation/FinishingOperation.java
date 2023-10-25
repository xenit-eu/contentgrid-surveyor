package com.contentgrid.surveyor.spi.storage.aggregation;

import java.util.function.Function;
import lombok.NonNull;

public record FinishingOperation(
        @NonNull
        AggregationOperation operation
) implements Operation {

    @Override
    public <T> T perform(Function<BucketingOperation, T> bucketingOperation,
            Function<FinishingOperation, T> finishingOperation) {
        return finishingOperation.apply(this);
    }
}
