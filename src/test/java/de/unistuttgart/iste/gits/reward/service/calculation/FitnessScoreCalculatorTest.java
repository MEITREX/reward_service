package de.unistuttgart.iste.gits.reward.service.calculation;

import de.unistuttgart.iste.gits.common.event.UserProgressLogEvent;
import de.unistuttgart.iste.gits.generated.dto.*;
import de.unistuttgart.iste.gits.reward.persistence.dao.AllRewardScoresEntity;
import de.unistuttgart.iste.gits.reward.persistence.dao.RewardScoreEntity;
import de.unistuttgart.iste.gits.reward.persistence.dao.RewardScoreLogEntry;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Test class for {@link FitnessScoreCalculator}
 */
class FitnessScoreCalculatorTest {

    private final FitnessScoreCalculator fitnessScoreCalculator = new FitnessScoreCalculator();

    /**
     * Given a content due for repetition exist with one day due with a correctness of 100%
     * When recalculateScore is called
     * Then the fitness score is increased by the correct value and a log entry is added
     */
    @Test
    void testRecalculateScoresFullCorrectness() {
        AllRewardScoresEntity allRewardScores = createAllRewardScoresEntityWithFitnessOf(100);
        UUID contentId = UUID.randomUUID();
        List<Content> contents = List.of(
                createContentWithUserData(contentId, UserProgressData.builder()
                        .setNextLearnDate(OffsetDateTime.now())
                        .setLog(List.of(
                                ProgressLogItem.builder()
                                        .setTimestamp(OffsetDateTime.now())
                                        .setCorrectness(1)
                                        .setSuccess(true)
                                        .setHintsUsed(0)
                                        .build()
                        ))
                        .build())
        );

        RewardScoreEntity fitness = fitnessScoreCalculator.recalculateScore(allRewardScores, contents);

        assertThat(fitness.getValue(), is(99));
        assertThat(fitness.getLog(), hasSize(1));

        RewardScoreLogEntry logEntry = fitness.getLog().get(0);
        assertThat(logEntry.getDifference(), is(-1));
        assertThat(logEntry.getReason(), is(RewardChangeReason.CONTENT_DUE_FOR_REPETITION));
        assertThat(logEntry.getAssociatedContentIds(), contains(contentId));
        assertThat(logEntry.getOldValue(), is(100));
        assertThat(logEntry.getNewValue(), is(99));
    }

    /**
     * Given 5 contents exist that are 10 days due for repetition with a correctness of 95%
     * When recalculateScore is called
     * Then the fitness score is increased by the correct value and a log entry is added
     */
    @Test
    void testRecalculateScoresExampleCase() {
        AllRewardScoresEntity allRewardScores = createAllRewardScoresEntityWithFitnessOf(100);
        UUID contentId = UUID.randomUUID();
        List<Content> contents = Collections.nCopies(5,
                createContentWithUserData(contentId,
                        UserProgressData.builder()
                                .setNextLearnDate(OffsetDateTime.now().minusDays(10))
                                .setLog(List.of(
                                        ProgressLogItem.builder()
                                                .setTimestamp(OffsetDateTime.now())
                                                .setCorrectness(0.95)
                                                .setSuccess(true)
                                                .setHintsUsed(0)
                                                .build()
                                ))
                                .build())
        );

        RewardScoreEntity fitness = fitnessScoreCalculator.recalculateScore(allRewardScores, contents);

        assertThat(fitness.getValue(), is(84)); //100 - 5 * (1 + (2 * (10+1) * (1 - 0.95^2)))
        assertThat(fitness.getLog(), hasSize(1));

        RewardScoreLogEntry logEntry = fitness.getLog().get(0);
        assertThat(logEntry.getDifference(), is(-16));
        assertThat(logEntry.getReason(), is(RewardChangeReason.CONTENT_DUE_FOR_REPETITION));
        assertThat(logEntry.getAssociatedContentIds(), hasItem(contentId));
        assertThat(logEntry.getAssociatedContentIds(), hasSize(5));
        assertThat(logEntry.getOldValue(), is(100));
        assertThat(logEntry.getNewValue(), is(84));
    }

    /**
     * Given no contents exist
     * When recalculateScore is called
     * Then the fitness score is not changed and no log entry is added
     */
    @Test
    void testRecalculateScoreWithoutContent() {
        AllRewardScoresEntity allRewardScores = createAllRewardScoresEntityWithFitnessOf(100);

        RewardScoreEntity fitness = fitnessScoreCalculator.recalculateScore(allRewardScores, List.of());

        assertThat(fitness.getValue(), is(100));
        assertThat(fitness.getLog(), is(empty()));
    }

    /**
     * Given no content is due for repetition
     * When recalculateScore is called
     * Then the fitness score is not changed and no log entry is added
     */
    @Test
    void testRecalculateScoresWithoutContentsDueForRepetition() {
        AllRewardScoresEntity allRewardScores = createAllRewardScoresEntityWithFitnessOf(100);
        List<Content> contents = List.of(
                createContentWithUserData(
                        UserProgressData.builder()
                                .setNextLearnDate(OffsetDateTime.now().plusDays(1))
                                .setLog(logWithOneSuccessfulEntry())
                                .build()),
                createContentWithUserData(
                        UserProgressData.builder()
                                .setNextLearnDate(OffsetDateTime.now().plusDays(3))
                                .setLog(logWithOneSuccessfulEntry())
                                .build())
        );

        RewardScoreEntity fitness = fitnessScoreCalculator.recalculateScore(allRewardScores, contents);

        // should not change as no content is due for repetition
        assertThat(fitness.getValue(), is(100));
        assertThat(fitness.getLog(), is(empty()));
    }

    /**
     * Given a content without a repetition date
     * When recalculateScore is called
     * Then the fitness score is not changed and no log entry is added
     */
    @Test
    void testRecalculateScoresWithContentThatHasNoRepetitionDate() {
        AllRewardScoresEntity allRewardScores = createAllRewardScoresEntityWithFitnessOf(100);
        List<Content> contents = List.of(
                createContentWithUserData(
                        UserProgressData.builder()
                                .setNextLearnDate(null) // no repetition date, e.g. media content
                                .setLog(logWithOneSuccessfulEntry())
                                .build())
        );

        RewardScoreEntity fitness = fitnessScoreCalculator.recalculateScore(allRewardScores, contents);

        // should not change as no content is due for repetition
        assertThat(fitness.getValue(), is(100));
        assertThat(fitness.getLog(), is(empty()));
    }

    /**
     * Given only contents that were not learned yet exist
     * When recalculateScore is called
     * Then the fitness score is not changed and no log entry is added
     * <p>
     * Note: those contents should only affect health, not fitness
     */
    @Test
    void testRecalculateScoresWithNoContentsThatWereNotLearnedYet() {
        AllRewardScoresEntity allRewardScores = createAllRewardScoresEntityWithFitnessOf(100);
        List<Content> contents = List.of(
                createContentWithUserData(
                        UserProgressData.builder()
                                .setNextLearnDate(OffsetDateTime.now().minusDays(1))
                                .setLog(List.of()) // not learned yet
                                .build()),
                createContentWithUserData(
                        UserProgressData.builder()
                                .setNextLearnDate(OffsetDateTime.now().minusDays(3))
                                .setLog(List.of(ProgressLogItem.builder()
                                        .setTimestamp(OffsetDateTime.now().minusDays(3))
                                        .setCorrectness(0)
                                        .setSuccess(false)
                                        .setHintsUsed(0)
                                        .build())) // not learned successfully yet
                                .build())
        );

        RewardScoreEntity fitness = fitnessScoreCalculator.recalculateScore(allRewardScores, contents);

        // should not change as no content is due for repetition
        assertThat(fitness.getValue(), is(100));
        assertThat(fitness.getLog(), is(empty()));
    }

    /**
     * Given 2 contents are due for repetition and one of them gets repeated successfully
     * When calculateOnContentWorkedOn is called
     * Then the fitness score is increased and a log entry is added
     */
    @Test
    void calculateOnContentWorkedOn() {
        AllRewardScoresEntity allRewardScores = createAllRewardScoresEntityWithFitnessOf(50);
        UUID contentId = UUID.randomUUID();
        List<Content> contents = List.of(
                createContentWithUserData(contentId, UserProgressData.builder()
                        .setNextLearnDate(OffsetDateTime.now().minusDays(1))
                        .setLog(logWithOneSuccessfulEntry()) // learned successfully
                        .build()),
                createContentWithUserData(UserProgressData.builder()
                        .setNextLearnDate(OffsetDateTime.now().minusDays(1))
                        .setLog(logWithOneSuccessfulEntry())
                        .build()),
                createContentWithUserData(
                        UserProgressData.builder()
                                .setLog(List.of(ProgressLogItem.builder()
                                        .setTimestamp(OffsetDateTime.now().minusDays(3))
                                        .setCorrectness(0)
                                        .setSuccess(false)
                                        .setHintsUsed(0)
                                        .build())) // not learned successfully yet
                                .build())
        );
        UserProgressLogEvent event = UserProgressLogEvent.builder()
                .userId(UUID.randomUUID())
                .contentId(contentId)
                .correctness(1)
                .hintsUsed(0)
                .success(true)
                .build();

        RewardScoreEntity fitness = fitnessScoreCalculator.calculateOnContentWorkedOn(allRewardScores, contents, event);

        // should not change as no content is due for repetition
        assertThat(fitness.getValue(), is(75));
        assertThat(fitness.getLog(), hasSize(1));

        RewardScoreLogEntry logItem = fitness.getLog().get(0);
        assertThat(logItem.getDifference(), is(25));
        assertThat(logItem.getReason(), is(RewardChangeReason.CONTENT_REVIEWED));
        assertThat(logItem.getAssociatedContentIds(), contains(contentId));
        assertThat(logItem.getOldValue(), is(50));
        assertThat(logItem.getNewValue(), is(75));
    }

    /**
     * Given 2 contents are due for repetition and one of them gets repeated unsuccessfully
     * When calculateOnContentWorkedOn is called
     * Then the fitness score is not changed and no log entry is added
     */
    @Test
    void testUnsuccessfulRepetitionsDontRewardFitness() {
        AllRewardScoresEntity allRewardScores = createAllRewardScoresEntityWithFitnessOf(50);
        UUID contentId = UUID.randomUUID();
        List<Content> contents = List.of(
                createContentWithUserData(contentId, UserProgressData.builder()
                        .setNextLearnDate(OffsetDateTime.now().minusDays(1))
                        .setLog(logWithOneSuccessfulEntry()) // learned successfully
                        .build()),
                createContentWithUserData(UserProgressData.builder()
                        .setNextLearnDate(OffsetDateTime.now().minusDays(1))
                        .setLog(logWithOneSuccessfulEntry())
                        .build())
        );
        UserProgressLogEvent event = UserProgressLogEvent.builder()
                .userId(UUID.randomUUID())
                .contentId(contentId)
                .correctness(0.5)
                .hintsUsed(0)
                .success(false)
                .build();

        RewardScoreEntity fitness = fitnessScoreCalculator.calculateOnContentWorkedOn(allRewardScores, contents, event);

        // should not change as no content is due for repetition
        assertThat(fitness.getValue(), is(50));
        assertThat(fitness.getLog(), is(empty()));
    }

    /**
     * Given 2 contents that are not due for repetition but one of them gets repeated successfully
     * When calculateOnContentWorkedOn is called
     * Then the fitness score is increased by 1 and a log entry is added
     */
    @Test
    void testRepeatingContentsThatAreNotDueForLearningRewards1Fitness() {
        AllRewardScoresEntity allRewardScores = createAllRewardScoresEntityWithFitnessOf(50);
        UUID contentId = UUID.randomUUID();
        List<Content> contents = List.of(
                createContentWithUserData(contentId, UserProgressData.builder()
                        .setNextLearnDate(OffsetDateTime.now().plusDays(1))
                        .setLog(logWithOneSuccessfulEntry()) // learned successfully
                        .build()),
                createContentWithUserData(UserProgressData.builder()
                        .setNextLearnDate(OffsetDateTime.now().plusDays(1))
                        .setLog(logWithOneSuccessfulEntry())
                        .build())
        );
        UserProgressLogEvent event = UserProgressLogEvent.builder()
                .userId(UUID.randomUUID())
                .contentId(contentId)
                .correctness(1)
                .hintsUsed(0)
                .success(true)
                .build();

        RewardScoreEntity fitness = fitnessScoreCalculator.calculateOnContentWorkedOn(allRewardScores, contents, event);

        assertThat(fitness.getValue(), is(51));
        assertThat(fitness.getLog(), hasSize(1));

        RewardScoreLogEntry logItem = fitness.getLog().get(0);
        assertThat(logItem.getDifference(), is(1));
        assertThat(logItem.getReason(), is(RewardChangeReason.CONTENT_REVIEWED));
        assertThat(logItem.getAssociatedContentIds(), contains(contentId));
        assertThat(logItem.getOldValue(), is(50));
        assertThat(logItem.getNewValue(), is(51));
    }

    /**
     * Given 2 contents that are due for repetition
     * When one of them gets repeated successfully twice
     * Then only the first repetition rewards fitness
     */
    @Test
    void testRepeatingContentMoreThanOnceADayWillNotRewardFitness() {
        AllRewardScoresEntity allRewardScores = createAllRewardScoresEntityWithFitnessOf(50);
        UUID contentId = UUID.randomUUID();
        List<Content> contents = List.of(
                // one content that was learned 10 minutes ago
                createContentWithUserData(contentId, UserProgressData.builder()
                        .setNextLearnDate(OffsetDateTime.now().minusDays(1))
                        .setLog(List.of(ProgressLogItem.builder()
                                .setTimestamp(OffsetDateTime.now().minusMinutes(10))
                                .setSuccess(true)
                                .build()))
                        .build()),
                createContentWithUserData(UserProgressData.builder()
                        .setNextLearnDate(OffsetDateTime.now().minusDays(1))
                        .setLog(logWithOneSuccessfulEntry())
                        .build())
        );
        UserProgressLogEvent event = UserProgressLogEvent.builder()
                .userId(UUID.randomUUID())
                .contentId(contentId)
                .correctness(1)
                .hintsUsed(0)
                .success(true)
                .build();

        RewardScoreEntity fitness = fitnessScoreCalculator.calculateOnContentWorkedOn(allRewardScores, contents, event);

        assertThat(fitness.getValue(), is(50));
        assertThat(fitness.getLog(), is(empty()));
    }

    /**
     * Given 2 contents that are due for repetition, one having a correctness of 0.5
     * When one of them gets repeated successfully
     * Then the fitness score increase in proportion to the correctness of the repetition
     */
    @Test
    void testCorrectnessInfluencesFitnessReward() {
        AllRewardScoresEntity allRewardScores = createAllRewardScoresEntityWithFitnessOf(0);
        UUID contentId = UUID.randomUUID();
        List<Content> contents = List.of(
                createContentWithUserData(contentId, UserProgressData.builder()
                        .setNextLearnDate(OffsetDateTime.now().minusDays(1))
                        .setLog(List.of(
                                ProgressLogItem.builder()
                                        .setTimestamp(OffsetDateTime.now().minusDays(1))
                                        .setCorrectness(0.5)
                                        .setSuccess(true)
                                        .setHintsUsed(0)
                                        .build()))
                        .build()),
                createContentWithUserData(UserProgressData.builder()
                        .setNextLearnDate(OffsetDateTime.now().minusDays(1))
                        .setLog(logWithOneSuccessfulEntry())
                        .build())
        );
        UserProgressLogEvent event = UserProgressLogEvent.builder()
                .userId(UUID.randomUUID())
                .contentId(contentId)
                .correctness(1.0) // increase correctness by 50%
                .hintsUsed(0)
                .success(true)
                .build();

        RewardScoreEntity fitness = fitnessScoreCalculator.calculateOnContentWorkedOn(allRewardScores, contents, event);

        assertThat(fitness.getValue(), is(75)); // 50 + (1 - 0.5) * 50
        assertThat(fitness.getLog(), hasSize(1));

        allRewardScores = createAllRewardScoresEntityWithFitnessOf(0);
        event = UserProgressLogEvent.builder()
                .userId(UUID.randomUUID())
                .contentId(contentId)
                .correctness(0.0) // decrease correctness by 50%
                .hintsUsed(0)
                .success(true)
                .build();

        fitness = fitnessScoreCalculator.calculateOnContentWorkedOn(allRewardScores, contents, event);

        assertThat(fitness.getValue(), is(25)); // 50 + (0 - 0.5) * 50
        assertThat(fitness.getLog(), hasSize(1));
    }

    private List<ProgressLogItem> logWithOneSuccessfulEntry() {
        return List.of(
                ProgressLogItem.builder()
                        .setTimestamp(OffsetDateTime.now().minusDays(1))
                        .setCorrectness(1)
                        .setSuccess(true)
                        .setHintsUsed(0)
                        .build()
        );
    }

    private Content createContentWithUserData(UserProgressData userProgressData) {
        return createContentWithUserData(UUID.randomUUID(), userProgressData);
    }

    private Content createContentWithUserData(UUID contentId, UserProgressData userProgressData) {
        return FlashcardSetAssessment.builder()
                .setId(contentId)
                .setMetadata(ContentMetadata.builder().build())
                .setAssessmentMetadata(AssessmentMetadata.builder().build())
                .setUserProgressData(userProgressData)
                .build();
    }

    private AllRewardScoresEntity createAllRewardScoresEntityWithFitnessOf(int fitness) {
        return AllRewardScoresEntity.builder()
                .health(RewardScoreEntity.builder().value(100).build())
                .fitness(RewardScoreEntity.builder().value(fitness).build())
                .growth(RewardScoreEntity.builder().value(0).build())
                .strength(RewardScoreEntity.builder().value(0).build())
                .power(RewardScoreEntity.builder().value(0).build())
                .build();
    }
}
