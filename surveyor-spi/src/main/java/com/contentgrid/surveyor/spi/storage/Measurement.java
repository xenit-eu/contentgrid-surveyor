package com.contentgrid.surveyor.spi.storage;

import com.contentgrid.surveyor.spi.TimeInterval;
import com.contentgrid.surveyor.spi.resources.Metric;
import java.math.BigDecimal;
import lombok.NonNull;
import lombok.Value;

@Value
public class Measurement {

    @NonNull
    TimeInterval measureInterval;
    @NonNull
    Metric metric;
    @NonNull
    BigDecimal value;
}
