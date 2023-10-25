package com.contentgrid.surveyor.spi.storage;

import com.contentgrid.surveyor.spi.ResourceDefinition;
import com.contentgrid.surveyor.spi.TimeInterval;
import java.util.Optional;

public interface LastEventCountMetricSpiPort {

    Optional<TimeInterval> getLastEventCountMetricInterval(ResourceDefinition resourceDefinition);
}
