package de.unistuttgart.iste.gits.reward.service.calculation;

import de.unistuttgart.iste.gits.common.event.UserProgressLogEvent;
import de.unistuttgart.iste.gits.generated.dto.Content;
import de.unistuttgart.iste.gits.generated.dto.RewardChangeReason;
import de.unistuttgart.iste.gits.reward.persistence.dao.RewardScoreEntity;
import de.unistuttgart.iste.gits.reward.persistence.dao.RewardScoreLogEntry;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.springframework.util.CollectionUtils.isEmpty;

@Component
public class HealthScoreCalculator implements ScoreCalculator {

    public static final double HEALTH_MODIFIER_PER_DAY = 0.5;
    public static final double HEALTH_DECREASE_CAP = -20.0;

    @Override
    public RewardScoreEntity recalculateScore(RewardScoreEntity rewardScore,
                                              List<Content> contents) {
        int oldScore = rewardScore.getValue();
        OffsetDateTime today = OffsetDateTime.now();

        List<Content> newDueContents = getDueContentsThatWereNeverWorked(contents, today);

        int diff = calculateHealthDecrease(newDueContents, today);

        if (diff == 0) {
            return rewardScore;
        }

        RewardScoreLogEntry logEntry = RewardScoreLogEntry.builder()
                .date(today)
                .difference(diff)
                .newValue(oldScore + diff)
                .oldValue(oldScore)
                .reason(RewardChangeReason.CONTENT_DUE_FOR_LEARNING)
                .associatedContents(getIds(newDueContents))
                .build();

        rewardScore.setValue(oldScore + diff);
        rewardScore.getLog().add(logEntry);

        return rewardScore;
    }

    @Override
    public RewardScoreEntity calculateOnContentWorkedOn(
            RewardScoreEntity rewardScore,
            List<Content> contents,
            UserProgressLogEvent event) {

        int oldScore = rewardScore.getValue();
        int diffToFull = 100 - oldScore;

        if (diffToFull == 0) {
            return rewardScore;
        }

        OffsetDateTime today = OffsetDateTime.now();

        List<Content> newDueContents = getDueContentsThatWereNeverWorked(contents, today);
        int numberOfNewDueContentsBefore = newDueContents.size();
        // this list might or might not include the content that was just worked on
        // depending on if the content service has already processed the event or not
        if (!doesListContainContentWithId(newDueContents, event.getContentId())) {
            numberOfNewDueContentsBefore++;
        }

        int healthIncrease = diffToFull / numberOfNewDueContentsBefore;
        int newValue = Math.min(oldScore + healthIncrease, 100);

        RewardScoreLogEntry logEntry = RewardScoreLogEntry.builder()
                .date(today)
                .difference(healthIncrease)
                .newValue(newValue)
                .oldValue(oldScore)
                .reason(RewardChangeReason.CONTENT_DONE)
                .associatedContents(List.of(event.getContentId()))
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
     * @return a negative(!) number representing the health decrease
     */
    private int calculateHealthDecrease(List<Content> newDueContents, OffsetDateTime today) {
        return (int) Math.max(HEALTH_DECREASE_CAP,
                Math.floor(HEALTH_MODIFIER_PER_DAY * newDueContents.stream()
                        .mapToInt(content -> getDaysOverDue(content, today))
                        .map(days -> days + 1) // on the day it is due, it should count as 1 day overdue
                        .map(days -> days * -1) // negative because it is a decrease
                        .sum()));
    }

    private List<Content> getDueContentsThatWereNeverWorked(List<Content> contents, OffsetDateTime today) {
        return contents.stream()
                .filter(this::isContentNotWorkedOn)
                .filter(content -> isContentDue(content, today))
                .toList();
    }

    private boolean isContentNotWorkedOn(Content content) {
        return isEmpty(content.getUserProgressData().getLog());
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
}
