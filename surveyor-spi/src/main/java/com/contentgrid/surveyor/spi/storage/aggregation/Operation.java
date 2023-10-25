package com.contentgrid.surveyor.spi.storage.aggregation;

import java.util.function.Function;

public interface Operation {

    <T> T perform(
            Function<BucketingOperation, T> bucketingOperation,
            Function<FinishingOperation, T> finishingOperation
    );
}
