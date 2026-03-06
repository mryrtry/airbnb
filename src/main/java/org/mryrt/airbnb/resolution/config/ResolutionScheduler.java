package org.mryrt.airbnb.resolution.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mryrt.airbnb.resolution.service.ResolutionService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ResolutionScheduler {

    private final ResolutionService resolutionService;

    @Scheduled(cron = "${airbnb.resolution.scheduler.cron:0 0 * * * *}")
    public void closeExpiredResolutionWindows() {
        resolutionService.closeExpiredWindows();
        log.debug("Resolution scheduler: closeExpiredWindows completed");
    }
}
