package com.contentgrid.surveyor.drivers.schedule;

import com.contentgrid.surveyor.api.pull.PullMetrics;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;

@RequiredArgsConstructor
public class ScheduledPullMetricsComponent {

    private final PullMetrics pullMetrics;

    @Scheduled(fixedRateString = "PT1M")
    void pullMetrics() {
        pullMetrics.pullMetrics();
    }

}
