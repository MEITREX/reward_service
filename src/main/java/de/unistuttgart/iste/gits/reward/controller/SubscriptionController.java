package de.unistuttgart.iste.gits.reward.controller;

import de.unistuttgart.iste.gits.common.event.ContentProgressedEvent;
import de.unistuttgart.iste.gits.common.event.CourseChangeEvent;
import de.unistuttgart.iste.gits.generated.dto.RewardScores;
import de.unistuttgart.iste.gits.reward.service.RewardService;
import io.dapr.Topic;
import io.dapr.client.domain.CloudEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@Slf4j
public class SubscriptionController {

    private final RewardService rewardService;

    /**
     * Event handler for the user-progress-updated event
     */
    @Topic(name = "user-progress-updated", pubsubName = "gits")
    @PostMapping(path = "/reward-service/user-progress-pubsub")
    public Mono<RewardScores> onUserProgress(@RequestBody final CloudEvent<ContentProgressedEvent> cloudEvent) {
        log.info("Received event: {}", cloudEvent.getData());
        return Mono.fromCallable(() -> {
            try {
                return rewardService.calculateScoresOnContentWorkedOn(cloudEvent.getData());
            } catch (final Exception e) {
                log.error("Error while processing user progress event", e);
                return null;
            }
        });
    }

    /**
     * Event handler for when a course is deleted
     *
     * @param cloudEvent the cloud event
     */
    @Topic(name = "course-changed", pubsubName = "gits")
    @PostMapping(path = "/reward-service/course-changed-pubsub")
    public Mono<Void> updateAssociation(@RequestBody final CloudEvent<CourseChangeEvent> cloudEvent) {

        return Mono.fromRunnable(
                () -> {
                    try {
                        rewardService.removeRewardData(cloudEvent.getData());
                    } catch (final Exception e) {
                        log.error(e.getMessage());
                    }
                });
    }
}
