package com.contentgrid.surveyor.spi.config;

import com.contentgrid.surveyor.spi.MetricCollectorSystemType;
import java.util.List;

public interface FindCollectionConfigurationsSpiPort {

    List<MetricCollectionConfig> findConfigurationsFor(MetricCollectorSystemType sourceSystemType);
}
