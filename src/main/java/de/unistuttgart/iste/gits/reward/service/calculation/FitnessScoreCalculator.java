package de.unistuttgart.iste.gits.reward.service.calculation;

import de.unistuttgart.iste.gits.common.event.UserProgressLogEvent;
import de.unistuttgart.iste.gits.generated.dto.Content;
import de.unistuttgart.iste.gits.generated.dto.ProgressLogItem;
import de.unistuttgart.iste.gits.generated.dto.RewardChangeReason;
import de.unistuttgart.iste.gits.reward.persistence.dao.AllRewardScoresEntity;
import de.unistuttgart.iste.gits.reward.persistence.dao.RewardScoreEntity;
import de.unistuttgart.iste.gits.reward.persistence.dao.RewardScoreLogEntry;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Component
public class FitnessScoreCalculator implements ScoreCalculator {
    private static final double MAX_DECREASE_PER_DAY = 20;

    @Override
    public RewardScoreEntity recalculateScore(AllRewardScoresEntity allRewardScores, List<Content> contents) {
        RewardScoreEntity fitnessScoreBefore = allRewardScores.getFitness();

        double fitnessDecrease = calculateFitnessDecrease(contents);
        double newFitnessScore = fitnessScoreBefore.getValue() - fitnessDecrease;
        int intNewFitnessScore = (int) Math.round(newFitnessScore);

        if (fitnessDecrease == 0) {
            return fitnessScoreBefore;
        }

        RewardScoreLogEntry logEntry = RewardScoreLogEntry.builder()
                .date(OffsetDateTime.now())
                .difference(intNewFitnessScore - fitnessScoreBefore.getValue())
                .oldValue(fitnessScoreBefore.getValue())
                .newValue(intNewFitnessScore)
                .reason(RewardChangeReason.CONTENT_DUE_FOR_REPETITION)
                .associatedContentIds(getIds(contents))
                .build();

        fitnessScoreBefore.setValue(intNewFitnessScore);
        fitnessScoreBefore.getLog().add(logEntry);

        return fitnessScoreBefore;
    }

    @Override
    public RewardScoreEntity calculateOnContentWorkedOn(AllRewardScoresEntity allRewardScores, List<Content> contents, UserProgressLogEvent event) {
        RewardScoreEntity fitnessScoreBefore = allRewardScores.getFitness();

        Content content = getContentOfEvent(contents, event);
        List<Content> contentsDueForReview = getContentsToRepeat(contents);

        ProgressLogItem latestReview = getLatestReviewExcludingTriggerOfEvent(content);
        double correctnessBefore = latestReview.getCorrectness();
        double correctnessAfter = event.getCorrectness();

        double fitnessRegen = calculateFitnessRegeneration(fitnessScoreBefore.getValue(), contentsDueForReview.size(),
                correctnessBefore, correctnessAfter);

        if (fitnessRegen == 0
            || !event.isSuccess()
            || wasAlreadyLearnedToday(latestReview)
        ) {
            // no change in fitness score or content was not reviewed successfully
            // or content was already learned today
            return fitnessScoreBefore;
        }

        double updatedFitnessScore = fitnessScoreBefore.getValue() + fitnessRegen;
        int intUpdatedFitnessScore = (int) Math.round(updatedFitnessScore);

        if (!isDueForRepetition(content)) {
            // content was reviewed successfully but is not due for repetition
            intUpdatedFitnessScore = fitnessScoreBefore.getValue() + 1;
        }

        RewardScoreLogEntry logEntry = RewardScoreLogEntry.builder()
                .date(OffsetDateTime.now())
                .difference(intUpdatedFitnessScore - fitnessScoreBefore.getValue())
                .oldValue(fitnessScoreBefore.getValue())
                .newValue(intUpdatedFitnessScore)
                .reason(RewardChangeReason.CONTENT_REVIEWED)
                .associatedContentIds(List.of(event.getContentId()))
                .build();

        fitnessScoreBefore.setValue(intUpdatedFitnessScore);
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
        OffsetDateTime today = OffsetDateTime.now();
        OffsetDateTime repetitionDate = content.getUserProgressData().getNextLearnDate();

        // Check if the repetition date is today or in the past
        return repetitionDate != null && (repetitionDate.isBefore(today) || repetitionDate.isEqual(today));
    }

    private boolean isNotNew(Content content) {
        // check if the content has been learned before successfully
        // otherwise it is considered new and should not be considered for fitness decrease
        return content.getUserProgressData()
                .getLog()
                .stream()
                .anyMatch(ProgressLogItem::getSuccess);
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

    private ProgressLogItem getLatestReviewExcludingTriggerOfEvent(Content content) {
        return content.getUserProgressData()
                .getLog()
                .stream()
                // make sure that the latest review is not the one that triggered the event
                .filter(logItem -> Duration.between(logItem.getTimestamp(), OffsetDateTime.now()).toMinutes() > 5)
                .findFirst()
                .orElseThrow();
    }

    private double calculateFitnessRegeneration(double fitness, int contentsToRepeat, double correctnessBefore, double correctnessAfter) {
        return (1 + correctnessAfter - correctnessBefore) * (100 - fitness) / contentsToRepeat;
    }

}
