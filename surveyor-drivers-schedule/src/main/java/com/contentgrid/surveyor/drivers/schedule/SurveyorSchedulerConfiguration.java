package com.contentgrid.surveyor.drivers.schedule;

import com.contentgrid.surveyor.api.pull.PullMetrics;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
public class SurveyorSchedulerConfiguration {

    @Bean
    ScheduledPullMetricsComponent scheduledPullMetricsComponent(PullMetrics pullMetrics) {
        return new ScheduledPullMetricsComponent(pullMetrics);
    }

}
