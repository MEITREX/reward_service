package de.unistuttgart.iste.gits.reward.service.calculation;

import de.unistuttgart.iste.gits.common.event.UserProgressLogEvent;
import de.unistuttgart.iste.gits.generated.dto.*;
import de.unistuttgart.iste.gits.reward.persistence.entity.*;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

class HealthScoreCalculatorTest {

    private final HealthScoreCalculator healthScoreCalculator = new HealthScoreCalculator();

    /**
     * Given a content due for repetition exist with one day due
     * When recalculateScore is called
     * Then the health score is decreased by the correct value and a log entry is added
     */
    @Test
    void testRecalculateScores() {
        AllRewardScoresEntity rewardScoresEntity = createAllRewardScoresEntityWithHealthOf(100);
        UUID contentId = UUID.randomUUID();
        UserProgressData userProgressData = UserProgressData.builder().build();
        List<Content> contents = List.of(
                createContentWithUserData(contentId, userProgressData, 1)
        );

        RewardScoreEntity health = healthScoreCalculator.recalculateScore(rewardScoresEntity, contents);

        assertThat(health.getValue(), is(99));
        assertThat(health.getLog(), hasSize(1));

        RewardScoreLogEntry logEntry = health.getLog().get(0);
        assertThat(logEntry.getDifference(), is(-1));
        assertThat(logEntry.getReason(), is(RewardChangeReason.CONTENT_DUE_FOR_LEARNING));
        assertThat(logEntry.getAssociatedContentIds(), contains(contentId));
        assertThat(logEntry.getOldValue(), is(100));
        assertThat(logEntry.getNewValue(), is(99));
    }

    /**
     * Given the health score is 1 and a content is due for repetition
     * When recalculateScore is called
     * Then the health score is decreased by 1 and a log entry is added
     */
    @Test
    void testRecalculateScoreNotBelow0() {
        AllRewardScoresEntity rewardScoresEntity = createAllRewardScoresEntityWithHealthOf(1);
        UUID contentId = UUID.randomUUID();
        UserProgressData userProgressData = UserProgressData.builder().build();
        List<Content> contents = List.of(
                createContentWithUserData(contentId, userProgressData, 100)
        );

        RewardScoreEntity health = healthScoreCalculator.recalculateScore(rewardScoresEntity, contents);

        assertThat(health.getValue(), is(0));
        assertThat(health.getLog(), hasSize(1));

        RewardScoreLogEntry logEntry = health.getLog().get(0);
        assertThat(logEntry.getDifference(), is(-1));
        assertThat(logEntry.getReason(), is(RewardChangeReason.CONTENT_DUE_FOR_LEARNING));
        assertThat(logEntry.getAssociatedContentIds(), contains(contentId));
        assertThat(logEntry.getOldValue(), is(1));
        assertThat(logEntry.getNewValue(), is(0));
    }

    /**
     * Given 5 contents exist that are 10 days due
     * When recalculateScore is called
     * Then the health score is decreased by the correct value and a log entry is added
     */
    @Test
    void testRecalculateScoreMaxHealthDecrease() {
        AllRewardScoresEntity allRewardScores = createAllRewardScoresEntityWithHealthOf(100);
        UUID contentId = UUID.randomUUID();
        List<Content> contents = Collections.nCopies(5,
                createContentWithUserData(contentId,
                        UserProgressData.builder().build(), 10)
        );

        RewardScoreEntity health = healthScoreCalculator.recalculateScore(allRewardScores, contents);

        assertThat(health.getValue(), is(80)); // Max health decrease 20%
        assertThat(health.getLog(), hasSize(1));

        RewardScoreLogEntry logEntry = health.getLog().get(0);
        assertThat(logEntry.getDifference(), is(-20));
        assertThat(logEntry.getReason(), is(RewardChangeReason.CONTENT_DUE_FOR_LEARNING));
        assertThat(logEntry.getAssociatedContentIds(), hasItem(contentId));
        assertThat(logEntry.getAssociatedContentIds(), hasSize(5));
        assertThat(logEntry.getOldValue(), is(100));
        assertThat(logEntry.getNewValue(), is(80));
    }

    /**
     * Given 2 contents exist that are 7 days due
     * When recalculateScore is called
     * Then the health score is decreased by the correct value and a log entry is added
     */
    @Test
    void testRecalculateScore() {
        AllRewardScoresEntity allRewardScores = createAllRewardScoresEntityWithHealthOf(100);
        UUID contentId = UUID.randomUUID();
        List<Content> contents = Collections.nCopies(2,
                createContentWithUserData(contentId,
                        UserProgressData.builder().build(), 7)
        );

        RewardScoreEntity health = healthScoreCalculator.recalculateScore(allRewardScores, contents);

        assertThat(health.getValue(), is(92)); //100 - 0.5 * 2 * 8
        assertThat(health.getLog(), hasSize(1));

        RewardScoreLogEntry logEntry = health.getLog().get(0);
        assertThat(logEntry.getDifference(), is(-8));
        assertThat(logEntry.getReason(), is(RewardChangeReason.CONTENT_DUE_FOR_LEARNING));
        assertThat(logEntry.getAssociatedContentIds(), hasItem(contentId));
        assertThat(logEntry.getAssociatedContentIds(), hasSize(2));
        assertThat(logEntry.getOldValue(), is(100));
        assertThat(logEntry.getNewValue(), is(92));
    }

    /**
     * Given 1 content is due for learning
     * When calculateOnContentWorkedOn is called
     * Then the health score is increased and a log entry is added
     */
    @Test
    void calculateOnContentWorkedOn() {
        AllRewardScoresEntity allRewardScores = createAllRewardScoresEntityWithHealthOf(50);
        UUID contentId = UUID.randomUUID();
        List<Content> contents = List.of(
                createContentWithUserData(contentId, UserProgressData.builder().build(), 1)
        );
        UserProgressLogEvent event = UserProgressLogEvent.builder()
                .userId(UUID.randomUUID())
                .contentId(contentId)
                .correctness(1)
                .hintsUsed(0)
                .success(true)
                .build();

        RewardScoreEntity health = healthScoreCalculator.calculateOnContentWorkedOn(allRewardScores, contents, event);

        // should be 100 due to no contents being due
        assertThat(health.getValue(), is(100));
        assertThat(health.getLog(), hasSize(1));

        RewardScoreLogEntry logItem = health.getLog().get(0);
        assertThat(logItem.getDifference(), is(50));
        assertThat(logItem.getReason(), is(RewardChangeReason.CONTENT_DONE));
        assertThat(logItem.getAssociatedContentIds(), contains(contentId));
        assertThat(logItem.getOldValue(), is(50));
        assertThat(logItem.getNewValue(), is(100));
    }

    /**
     * Given the health score is 100
     * When calculateOnContentWorkedOn is called
     * Then the health score is not changed and no log entry is added
     */
    @Test
    void calculateOnContentWorkedOnNotExceeding100() {
        AllRewardScoresEntity allRewardScores = createAllRewardScoresEntityWithHealthOf(100);
        UUID contentId = UUID.randomUUID();
        List<Content> contents = List.of(
                createContentWithUserData(contentId, UserProgressData.builder().build(), 1)
        );
        UserProgressLogEvent event = UserProgressLogEvent.builder()
                .userId(UUID.randomUUID())
                .contentId(contentId)
                .correctness(1)
                .hintsUsed(0)
                .success(true)
                .build();

        RewardScoreEntity health = healthScoreCalculator.calculateOnContentWorkedOn(allRewardScores, contents, event);

        // should be 100 due to no contents being due
        assertThat(health.getValue(), is(100));
        assertThat(health.getLog(), hasSize(0));
    }

    /**
     * Given no contents exist
     * When recalculateScore is called
     * Then the health score is not changed and no log entry is added
     */
    @Test
    void testRecalculateScoreWithoutContent() {
        AllRewardScoresEntity allRewardScores = createAllRewardScoresEntityWithHealthOf(100);

        RewardScoreEntity health = healthScoreCalculator.recalculateScore(allRewardScores, List.of());

        assertThat(health.getValue(), is(100));
        assertThat(health.getLog(), is(empty()));
    }

    /**
     * Given no content is due for learning
     * When recalculateScore is called
     * Then the health score is not changed and no log entry is added
     */
    @Test
    void testRecalculateScoresWithoutContentsDueForLearning() {
        AllRewardScoresEntity allRewardScores = createAllRewardScoresEntityWithHealthOf(100);
        UUID contentId = UUID.randomUUID();
        List<Content> contents = List.of(
                createContentWithUserData(contentId,
                        UserProgressData.builder().build(), -1)
        );

        RewardScoreEntity health = healthScoreCalculator.recalculateScore(allRewardScores, contents);

        // should not change as no content is due for repetition
        assertThat(health.getValue(), is(100));
        assertThat(health.getLog(), is(empty()));
    }

    private Content createContentWithUserData(UUID contentId, UserProgressData userProgressData, int overdue) {
        return FlashcardSetAssessment.builder()
                .setId(contentId)
                .setMetadata(ContentMetadata.builder().setSuggestedDate(OffsetDateTime.now().minusDays(overdue)).build())
                .setAssessmentMetadata(AssessmentMetadata.builder().build())
                .setUserProgressData(userProgressData)
                .build();
    }

    private AllRewardScoresEntity createAllRewardScoresEntityWithHealthOf(int health) {
        return AllRewardScoresEntity.builder()
                .health(RewardScoreEntity.builder().value(health).build())
                .fitness(RewardScoreEntity.builder().value(100).build())
                .growth(RewardScoreEntity.builder().value(0).build())
                .strength(RewardScoreEntity.builder().value(0).build())
                .power(RewardScoreEntity.builder().value(0).build())
                .build();
    }
    @Test
    void testCalculateInitialHealthValueForNewEntity() {
        // Create a list of contents with different overdue days
        List<Content> contents = new ArrayList<>();
        contents.add(createContentWithUserData(UUID.randomUUID(), UserProgressData.builder().build(), 1));
        contents.add(createContentWithUserData(UUID.randomUUID(), UserProgressData.builder().build(), 5));
        contents.add(createContentWithUserData(UUID.randomUUID(), UserProgressData.builder().build(), 10));

        // Calculate the initial health value using the method
        int initialHealthValue = healthScoreCalculator.calculateInitialHealthValueForNewEntity(contents);

        // Calculate expected initial health value based on your logic
        int expectedInitialHealthValue = 91;
        assertThat(initialHealthValue, is(expectedInitialHealthValue));
    }
}
