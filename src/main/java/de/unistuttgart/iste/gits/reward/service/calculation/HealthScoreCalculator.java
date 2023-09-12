package de.unistuttgart.iste.gits.reward.service.calculation;

import de.unistuttgart.iste.gits.common.event.UserProgressLogEvent;
import de.unistuttgart.iste.gits.generated.dto.Content;
import de.unistuttgart.iste.gits.generated.dto.RewardChangeReason;
import de.unistuttgart.iste.gits.reward.persistence.entity.*;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Component
public class HealthScoreCalculator implements ScoreCalculator {

    public static final double HEALTH_MODIFIER_PER_DAY = 0.5;
    public static final double HEALTH_DECREASE_CAP = 20.0;

    @Override
    public RewardScoreEntity recalculateScore(AllRewardScoresEntity allRewardScores,
                                              List<Content> contents) {
        RewardScoreEntity rewardScore = allRewardScores.getHealth();
        int oldScore = rewardScore.getValue();
        OffsetDateTime today = OffsetDateTime.now();

        List<Content> newDueContents = getDueContentsThatWereNeverWorked(contents, today);

        int diff = calculateHealthDecrease(newDueContents, today);
        int newValue = Math.max(0, oldScore - diff);

        if (newValue - oldScore == 0) {
            return rewardScore;
        }

        RewardScoreLogEntry logEntry = RewardScoreLogEntry.builder()
                .date(today)
                .difference(newValue - oldScore)
                .newValue(newValue)
                .oldValue(oldScore)
                .reason(RewardChangeReason.CONTENT_DUE_FOR_LEARNING)
                .associatedContentIds(getIds(newDueContents))
                .build();

        rewardScore.setValue(newValue);
        rewardScore.getLog().add(logEntry);

        return rewardScore;
    }

    @Override
    public RewardScoreEntity calculateOnContentWorkedOn(
            AllRewardScoresEntity allRewardScoresEntity,
            List<Content> contents,
            UserProgressLogEvent event) {
        RewardScoreEntity rewardScore = allRewardScoresEntity.getHealth();

        int oldScore = rewardScore.getValue();
        int diffToFull = 100 - oldScore;

        if (diffToFull == 0) {
            return rewardScore;
        }

        OffsetDateTime today = OffsetDateTime.now();

        List<Content> newDueContents = getDueContentsThatWereNeverWorked(contents, today);
        int numberOfNewDueContentsBefore = newDueContents.size();

        // just in case that the content list does not contain the content of the event
        if (!doesListContainContentWithId(newDueContents, event.getContentId())) {
            numberOfNewDueContentsBefore++;
        }

        int healthIncrease = diffToFull / numberOfNewDueContentsBefore;
        int newValue = Math.min(oldScore + healthIncrease, 100);

        RewardScoreLogEntry logEntry = RewardScoreLogEntry.builder()
                .date(today)
                .difference(newValue - oldScore)
                .newValue(newValue)
                .oldValue(oldScore)
                .reason(RewardChangeReason.CONTENT_DONE)
                .associatedContentIds(List.of(event.getContentId()))
                .build();

        rewardScore.setValue(newValue);
        rewardScore.getLog().add(logEntry);

        return rewardScore;

    }

    private static boolean doesListContainContentWithId(List<Content> newDueContents, UUID contentId) {
        return newDueContents.stream()
                .anyMatch(content -> content.getId().equals(contentId));
    }

    private static List<UUID> getIds(List<Content> newDueContents) {
        return newDueContents.stream()
                .map(Content::getId)
                .toList();
    }

    /**
     * Calculates the health decrease based on the number of days the content is overdue.
     * The decrease is capped at {@link #HEALTH_DECREASE_CAP}.
     *
     * @param newDueContents the contents that are due but were never worked on
     * @param today          the current date
     * @return a positive number representing the health decrease
     */
    private int calculateHealthDecrease(List<Content> newDueContents, OffsetDateTime today) {
        return (int) Math.min(HEALTH_DECREASE_CAP,
                Math.floor(HEALTH_MODIFIER_PER_DAY * newDueContents.stream()
                        .mapToInt(content -> getDaysOverDue(content, today))
                        .map(days -> days + 1) // on the day it is due, it should count as 1 day overdue
                        .sum()));
    }

    private List<Content> getDueContentsThatWereNeverWorked(List<Content> contents, OffsetDateTime today) {
        return contents.stream()
                .filter(this::isContentNew)
                .filter(content -> isContentDue(content, today))
                .toList();
    }

    private boolean isContentNew(Content content) {
        return !content.getUserProgressData().getIsLearned();
    }

    private boolean isContentDue(Content content, OffsetDateTime today) {
        OffsetDateTime dueDate = content.getMetadata().getSuggestedDate();
        return dueDate != null && dueDate.isBefore(today);
    }

    private int getDaysOverDue(Content content, OffsetDateTime today) {
        OffsetDateTime dueDate = content.getMetadata().getSuggestedDate();
        if (dueDate == null) {
            return 0;
        }
        return (int) Duration.between(today, dueDate).abs().toDays();
    }
    public int calculateInitialHealthValueForNewEntity(List<Content> contents) {
        OffsetDateTime today = OffsetDateTime.now();
        List<Content> newDueContents = getDueContentsThatWereNeverWorked(contents, today);
        int healthDecrease = calculateHealthDecrease(newDueContents, today);

        // Calculate initial health value based on overdue, never-worked-on contents
        int initialHealthValue = 100 - healthDecrease;

        // Ensure the initial health value is within bounds (0 to 100)
        return Math.max(0, Math.min(100, initialHealthValue));
    }


}
