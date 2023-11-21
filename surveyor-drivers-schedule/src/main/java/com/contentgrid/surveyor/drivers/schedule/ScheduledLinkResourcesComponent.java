package com.contentgrid.surveyor.drivers.schedule;

import com.contentgrid.surveyor.api.resources.LinkResources;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;

@RequiredArgsConstructor
public class ScheduledLinkResourcesComponent {

    private final LinkResources linkResources;

    @Scheduled(fixedRateString = "PT1M")
    void linkResources() {
        linkResources.linkUnlinkedResources();
    }

}
