package com.contentgrid.surveyor.spi.config;

import com.contentgrid.surveyor.spi.MetricSourceSystemType;
import java.util.List;

public interface FindCollectionConfigurationsSpiPort {
    List<MetricCollectionConfig> findConfigurationsFor(MetricSourceSystemType sourceSystemType);
}
