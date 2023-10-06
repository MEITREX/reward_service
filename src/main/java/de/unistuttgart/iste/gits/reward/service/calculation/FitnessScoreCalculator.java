package de.unistuttgart.iste.gits.reward.service.calculation;

import de.unistuttgart.iste.gits.common.event.ContentProgressedEvent;
import de.unistuttgart.iste.gits.generated.dto.Content;
import de.unistuttgart.iste.gits.generated.dto.ProgressLogItem;
import de.unistuttgart.iste.gits.generated.dto.RewardChangeReason;
import de.unistuttgart.iste.gits.reward.persistence.entity.AllRewardScoresEntity;
import de.unistuttgart.iste.gits.reward.persistence.entity.RewardScoreEntity;
import de.unistuttgart.iste.gits.reward.persistence.entity.RewardScoreLogEntry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Calculates the fitness score of a user, according the concept documented
 * <a href="https://gits-enpro.readthedocs.io/en/latest/dev-manuals/gamification/Scoring%20System.html#fitness">here</a>.
 */
@Component
@Slf4j
public class FitnessScoreCalculator implements ScoreCalculator {

    private static final int FITNESS_MAX = 100;
    private static final int FITNESS_MIN = 0;

    private static final double MAX_DECREASE_PER_DAY_DEFAULT = 20.0;

    private static final double FITNESS_MODIFIER_PER_DAY_DEFAULT = 2.0;

    private final double maxDecreasePerDay;

    private final double fitnessModifierPerDay;

    /**
     * Creates a new instance.
     *
     * @param maxDecreasePerDay     the maximum decrease per day
     * @param fitnessModifierPerDay the fitness modifier, applied to the number of days each content is overdue
     */
    @Autowired
    public FitnessScoreCalculator(@Value("${reward.fitness.max_decrease_per_day}") final double maxDecreasePerDay,
                                  @Value("${reward.fitness.multiplier}") final double fitnessModifierPerDay) {
        log.info("Creating FitnessScoreCalculator with maxDecreasePerDay={}, fitnessModifierPerDay={}",
                maxDecreasePerDay, fitnessModifierPerDay);
        this.maxDecreasePerDay = maxDecreasePerDay;
        this.fitnessModifierPerDay = fitnessModifierPerDay;
    }

    /**
     * Creates a new instance with default values.
     */
    public FitnessScoreCalculator() {
        this(MAX_DECREASE_PER_DAY_DEFAULT, FITNESS_MODIFIER_PER_DAY_DEFAULT);
    }

    @Override
    public RewardScoreEntity recalculateScore(final AllRewardScoresEntity allRewardScores, final List<Content> contents) {
        final RewardScoreEntity fitnessEntity = allRewardScores.getFitness();
        final int oldScore = fitnessEntity.getValue();

        final double fitnessDecrease = calculateFitnessDecrease(contents);
        final double newFitnessScore = Math.max(FITNESS_MIN, oldScore - fitnessDecrease);
        final int newFitnessRounded = (int) Math.round(newFitnessScore);

        if (newFitnessRounded - oldScore == 0) {
            // no change in fitness score, so no log entry is created
            return fitnessEntity;
        }

        final RewardScoreLogEntry logEntry = createLogEntryOnRecalculation(oldScore, newFitnessRounded, contents);

        fitnessEntity.setValue(newFitnessRounded);
        fitnessEntity.getLog().add(logEntry);

        return fitnessEntity;
    }

    @Override
    public RewardScoreEntity calculateOnContentWorkedOn(final AllRewardScoresEntity allRewardScores, final List<Content> contents, final ContentProgressedEvent event) {
        final RewardScoreEntity fitnessEntity = allRewardScores.getFitness();
        final int oldScore = fitnessEntity.getValue();

        final Content content = getContentOfEvent(contents, event);
        final List<Content> contentsDueForReview = getContentsToRepeat(contents);

        final Optional<ProgressLogItem> latestReview = getLatestReviewExcludingTriggerOfEvent(content);
        if (latestReview.isEmpty()) {
            // it is the first review of the content, so only health score is affected
            return fitnessEntity;
        }

        final double fitnessRegen = calculateFitnessRegeneration(oldScore, contentsDueForReview.size(), latestReview.get(), event);

        if (fitnessRegen == 0
            || !event.isSuccess()
            || wasAlreadyLearnedToday(latestReview.get())
        ) {
            // no change in fitness score or content was not reviewed successfully
            // or content was already learned today
            return fitnessEntity;
        }

        final int newFitnessScore = getNewFitnessScore(oldScore, fitnessRegen, content);

        final RewardScoreLogEntry logEntry = createLogEntryOnContentReviewed(oldScore, newFitnessScore, event.getContentId());

        fitnessEntity.setValue(newFitnessScore);
        fitnessEntity.getLog().add(logEntry);

        return fitnessEntity;
    }

    private int getNewFitnessScore(final int oldScore, final double fitnessRegen, final Content content) {
        int newFitnessScoreRounded = (int) Math.round(oldScore + fitnessRegen);

        if (!isDueForRepetition(content)) {
            // content was reviewed successfully but is not due for repetition
            newFitnessScoreRounded = oldScore + 1;
        }
        if (newFitnessScoreRounded >= FITNESS_MAX) {
            // fitness score cannot be higher than 100
            newFitnessScoreRounded = FITNESS_MAX;
        }
        return newFitnessScoreRounded;
    }

    private static RewardScoreLogEntry createLogEntryOnContentReviewed(final int oldScore,
                                                                       final int newScore,
                                                                       final UUID contentId) {
        return RewardScoreLogEntry.builder()
                .date(OffsetDateTime.now())
                .difference(newScore - oldScore)
                .oldValue(oldScore)
                .newValue(newScore)
                .reason(RewardChangeReason.CONTENT_REVIEWED)
                .associatedContentIds(List.of(contentId))
                .build();
    }


    private RewardScoreLogEntry createLogEntryOnRecalculation(final int oldScore,
                                                              final int newFitness,
                                                              final List<Content> contents) {
        return RewardScoreLogEntry.builder()
                .date(OffsetDateTime.now())
                .difference(newFitness - oldScore)
                .oldValue(oldScore)
                .newValue(newFitness)
                .reason(RewardChangeReason.CONTENT_DUE_FOR_REPETITION)
                .associatedContentIds(getIds(contents))
                .build();
    }


    private List<UUID> getIds(final List<Content> contents) {
        return contents.stream().map(Content::getId).toList();
    }

    private Content getContentOfEvent(final List<Content> contents, final ContentProgressedEvent event) {
        return contents.stream()
                .filter(content -> content.getId().equals(event.getContentId()))
                .findFirst()
                .orElseThrow();
    }

    private boolean wasAlreadyLearnedToday(final ProgressLogItem lastReview) {
        final OffsetDateTime lastReviewDate = lastReview.getTimestamp().truncatedTo(ChronoUnit.DAYS);
        final OffsetDateTime today = OffsetDateTime.now().truncatedTo(ChronoUnit.DAYS);

        // check if the last review was today
        return lastReviewDate.equals(today);
    }

    private double calculateFitnessDecrease(final List<Content> contents) {
        double fitnessDecrease = 0.0;

        for (final Content content : contents) {
            if (isDueForRepetition(content) && isNotNew(content)) {
                final int daysOverdue = calculateDaysOverdue(content);
                final double correctness = calculateCorrectnessModifier(getLatestReview(content));
                final double decreasePerDay = 1 + (fitnessModifierPerDay * daysOverdue * (1 - correctness));
                fitnessDecrease += decreasePerDay;
            }
        }

        return Math.min(maxDecreasePerDay, fitnessDecrease);
    }

    private boolean isDueForRepetition(final Content content) {
        return content.getUserProgressData().getIsDueForReview();
    }

    private boolean isNotNew(final Content content) {
        return content.getUserProgressData().getIsLearned();
    }

    private List<Content> getContentsToRepeat(final List<Content> contents) {
        return contents.stream()
                .filter(this::isDueForRepetition)
                .toList();
    }

    private int calculateDaysOverdue(final Content content) {
        final OffsetDateTime today = OffsetDateTime.now();
        final OffsetDateTime repetitionDate = content.getUserProgressData().getNextLearnDate();

        final long daysBetween = Duration.between(today, repetitionDate).abs().toDays();

        // add one because at the repetition date the content is already overdue
        return (int) daysBetween + 1;
    }


    private double calculateCorrectnessModifier(final ProgressLogItem logItem) {
        return Math.pow(logItem.getCorrectness(), 2);
    }

    private ProgressLogItem getLatestReview(final Content content) {
        return content.getUserProgressData()
                .getLog()
                .stream()
                .findFirst()
                .orElseThrow();
    }

    private Optional<ProgressLogItem> getLatestReviewExcludingTriggerOfEvent(final Content content) {
        return content.getUserProgressData()
                .getLog()
                .stream()
                // make sure that the latest review is not the one that triggered the event
                // for this we check if the timestamp of the review is more than 5 minutes in the past
                // this is not a perfect solution but is sufficient because multiple reviews per day
                // do not reward the user with more fitness points than one review per day
                .filter(logItem -> Duration.between(logItem.getTimestamp(), OffsetDateTime.now()).abs().toMinutes() > 5)
                .findFirst();
    }

    private double calculateFitnessRegeneration(final double fitness,
                                                final int contentsToRepeat,
                                                final ProgressLogItem progressLogItem,
                                                final ContentProgressedEvent event) {

        final double correctnessBefore = progressLogItem.getCorrectness();
        final double correctnessAfter = event.getCorrectness();

        // the user can gain more or less fitness points depending on the correctness of the review
        // compared to the correctness of the last review
        final double correctnessModifier = (1 + correctnessAfter - correctnessBefore);

        return correctnessModifier * (FITNESS_MAX - fitness) / contentsToRepeat;
    }

}
