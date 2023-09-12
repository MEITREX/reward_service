package de.unistuttgart.iste.gits.reward.service.calculation;

import de.unistuttgart.iste.gits.common.event.UserProgressLogEvent;
import de.unistuttgart.iste.gits.generated.dto.*;
import de.unistuttgart.iste.gits.reward.persistence.entity.*;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Calculates the fitness score of a user, according the concept documented
 * <a href="https://gits-enpro.readthedocs.io/en/latest/dev-manuals/gamification/Scoring%20System.html#fitness">here</a>.
 */
@Component
public class FitnessScoreCalculator implements ScoreCalculator {
    private static final double MAX_DECREASE_PER_DAY = 20;

    @Override
    public RewardScoreEntity recalculateScore(AllRewardScoresEntity allRewardScores, List<Content> contents) {
        RewardScoreEntity fitnessScoreBefore = allRewardScores.getFitness();

        double fitnessDecrease = calculateFitnessDecrease(contents);
        double newFitnessScore = Math.max(0.0, fitnessScoreBefore.getValue() - fitnessDecrease);
        int newFitnessRounded = (int) Math.round(newFitnessScore);

        if (newFitnessRounded - fitnessScoreBefore.getValue() == 0) {
            // no change in fitness score, so no log entry is created
            return fitnessScoreBefore;
        }

        RewardScoreLogEntry logEntry = RewardScoreLogEntry.builder()
                .date(OffsetDateTime.now())
                .difference(newFitnessRounded - fitnessScoreBefore.getValue())
                .oldValue(fitnessScoreBefore.getValue())
                .newValue(newFitnessRounded)
                .reason(RewardChangeReason.CONTENT_DUE_FOR_REPETITION)
                .associatedContentIds(getIds(contents))
                .build();

        fitnessScoreBefore.setValue(newFitnessRounded);
        fitnessScoreBefore.getLog().add(logEntry);

        return fitnessScoreBefore;
    }

    @Override
    public RewardScoreEntity calculateOnContentWorkedOn(AllRewardScoresEntity allRewardScores, List<Content> contents, UserProgressLogEvent event) {
        RewardScoreEntity fitnessScoreBefore = allRewardScores.getFitness();

        Content content = getContentOfEvent(contents, event);
        List<Content> contentsDueForReview = getContentsToRepeat(contents);

        Optional<ProgressLogItem> latestReview = getLatestReviewExcludingTriggerOfEvent(content);
        if (latestReview.isEmpty()) {
            // it is the first review of the content, so only health score is affected
            return fitnessScoreBefore;
        }

        ProgressLogItem latestReviewItem = latestReview.get();
        double correctnessBefore = latestReviewItem.getCorrectness();
        double correctnessAfter = event.getCorrectness();

        double fitnessRegen = calculateFitnessRegeneration(fitnessScoreBefore.getValue(), contentsDueForReview.size(),
                correctnessBefore, correctnessAfter);

        if (fitnessRegen == 0
            || !event.isSuccess()
            || wasAlreadyLearnedToday(latestReviewItem)
        ) {
            // no change in fitness score or content was not reviewed successfully
            // or content was already learned today
            return fitnessScoreBefore;
        }

        double updatedFitnessScore = fitnessScoreBefore.getValue() + fitnessRegen;
        int newFitnessScoreRounded = (int) Math.round(updatedFitnessScore);

        if (!isDueForRepetition(content)) {
            // content was reviewed successfully but is not due for repetition
            newFitnessScoreRounded = fitnessScoreBefore.getValue() + 1;
        }
        if (newFitnessScoreRounded >= 100) {
            // fitness score cannot be higher than 100
            newFitnessScoreRounded = 100;
        }

        RewardScoreLogEntry logEntry = RewardScoreLogEntry.builder()
                .date(OffsetDateTime.now())
                .difference(newFitnessScoreRounded - fitnessScoreBefore.getValue())
                .oldValue(fitnessScoreBefore.getValue())
                .newValue(newFitnessScoreRounded)
                .reason(RewardChangeReason.CONTENT_REVIEWED)
                .associatedContentIds(List.of(event.getContentId()))
                .build();

        fitnessScoreBefore.setValue(newFitnessScoreRounded);
        fitnessScoreBefore.getLog().add(logEntry);

        return fitnessScoreBefore;
    }

    private List<UUID> getIds(List<Content> contents) {
        return contents.stream().map(Content::getId).toList();
    }

    private Content getContentOfEvent(List<Content> contents, UserProgressLogEvent event) {
        return contents.stream()
                .filter(c -> c.getId().equals(event.getContentId()))
                .findFirst()
                .orElseThrow();
    }

    private boolean wasAlreadyLearnedToday(ProgressLogItem lastReview) {
        return lastReview.getTimestamp().truncatedTo(ChronoUnit.DAYS)
                .equals(OffsetDateTime.now().truncatedTo(ChronoUnit.DAYS));
    }

    private double calculateFitnessDecrease(List<Content> contents) {
        double fitnessDecrease = 0.0;

        for (Content content : contents) {
            if (isDueForRepetition(content) && isNotNew(content)) {
                int daysOverdue = calculateDaysOverdue(content);
                double correctness = calculateCorrectnessModifier(getLatestReview(content));
                double decreasePerDay = 1 + (2 * daysOverdue * (1 - correctness));
                fitnessDecrease += decreasePerDay;
            }
        }

        return Math.min(MAX_DECREASE_PER_DAY, fitnessDecrease);
    }

    private boolean isDueForRepetition(Content content) {
        return content.getUserProgressData().getIsDueForReview();
    }

    private boolean isNotNew(Content content) {
        return content.getUserProgressData().getIsLearned();
    }

    private List<Content> getContentsToRepeat(List<Content> contents) {
        return contents.stream()
                .filter(this::isDueForRepetition)
                .toList();
    }

    private int calculateDaysOverdue(Content content) {
        OffsetDateTime today = OffsetDateTime.now();
        OffsetDateTime repetitionDate = content.getUserProgressData().getNextLearnDate();

        long daysBetween = Duration.between(today, repetitionDate).abs().toDays();

        // add one because at the repetition date the content is already overdue
        return (int) daysBetween + 1;
    }


    private double calculateCorrectnessModifier(ProgressLogItem logItem) {
        return Math.pow(logItem.getCorrectness(), 2);
    }

    private ProgressLogItem getLatestReview(Content content) {
        return content.getUserProgressData()
                .getLog()
                .stream()
                .findFirst()
                .orElseThrow();
    }

    private Optional<ProgressLogItem> getLatestReviewExcludingTriggerOfEvent(Content content) {
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

    private double calculateFitnessRegeneration(double fitness, int contentsToRepeat, double correctnessBefore, double correctnessAfter) {
        return (1 + correctnessAfter - correctnessBefore) * (100 - fitness) / contentsToRepeat;
    }

}
