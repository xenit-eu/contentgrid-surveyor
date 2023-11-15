package com.contentgrid.surveyor.drivers.schedule;

import com.contentgrid.surveyor.api.pull.PullMetrics;
import com.contentgrid.surveyor.api.resources.LinkResources;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
public class SurveyorSchedulerConfiguration {

    @Bean
    ScheduledPullMetricsComponent scheduledPullMetricsComponent(PullMetrics pullMetrics) {
        return new ScheduledPullMetricsComponent(pullMetrics);
    }

    @Bean
    ScheduledLinkResourcesComponent scheduledLinkResourcesComponent(LinkResources linkResources) {
        return new ScheduledLinkResourcesComponent(linkResources);
    }

}
