package de.unistuttgart.iste.gits.reward.controller;

import de.unistuttgart.iste.gits.common.event.UserProgressLogEvent;
import de.unistuttgart.iste.gits.generated.dto.RewardScores;
import de.unistuttgart.iste.gits.reward.service.*;
import io.dapr.Topic;
import io.dapr.client.domain.CloudEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
public class SubscriptionController {

    private final RewardService rewardService;

    /**
     * Event handler for the content-progressed event
     */
    @Topic(name = "content-progressed", pubsubName = "gits")
    @PostMapping(path = "/reward-service/user-progress-pubsub")
    public Mono<RewardScores> onUserProgress(@RequestBody(required = false) CloudEvent<UserProgressLogEvent> cloudEvent,
                                             @RequestHeader Map<String, String> headers) {
        log.info("Received event: {}", cloudEvent.getData());
        return Mono.fromCallable(() -> {
            try {
                return rewardService.calculateScoresOnContentWorkedOn(cloudEvent.getData());
            } catch (CourseServiceClient.CourseServiceConnectionException |
                     ContentServiceClient.ContentServiceConnectionException e) {
                log.error("Error while processing user progress event", e);
                return null;
            }
        });
    }
}
