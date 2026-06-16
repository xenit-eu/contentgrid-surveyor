package com.contentgrid.surveyor.application.surveyor.actuator;

import com.contentgrid.surveyor.api.pull.PullMetrics;
import lombok.AllArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import reactor.core.publisher.Mono;

@AllArgsConstructor
public class SurveyorHealthActuator implements ReactiveHealthIndicator {

    private PullMetrics pullMetrics;

    @Override
    public Mono<Health> health() {
        return Mono.defer( () ->
            Mono.just(pullMetrics.isLastPullSuccesfull() ? Health.up().build() : Health.down().build())
        );
    }
}
