package de.unistuttgart.iste.gits.reward.service.calculation;

import de.unistuttgart.iste.gits.common.event.UserProgressUpdatedEvent;
import de.unistuttgart.iste.gits.generated.dto.Content;
import de.unistuttgart.iste.gits.generated.dto.RewardChangeReason;
import de.unistuttgart.iste.gits.reward.persistence.entity.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Calculates the growth score of a user, according the concept documented
 * <a href="https://gits-enpro.readthedocs.io/en/latest/dev-manuals/gamification/Scoring%20System.html#growth">here</a>.
 */
@Component
@Slf4j
public class GrowthScoreCalculator implements ScoreCalculator {
    @Override
    public RewardScoreEntity recalculateScore(final AllRewardScoresEntity allRewardScores,
                                              final List<Content> contents) {
        log.debug("Recalculating growth score for user {} in course {}",
                allRewardScores.getId().getUserId(), allRewardScores.getId().getCourseId());
        // growth score is not affected by recalculation
        // it is only affected by content worked on
        return allRewardScores.getGrowth();
    }

    @Override
    public RewardScoreEntity calculateOnContentWorkedOn(final AllRewardScoresEntity allRewardScores,
                                                        final List<Content> contents,
                                                        final UserProgressUpdatedEvent event) {
        log.debug("Calculating growth score for user {} in course {}",
                allRewardScores.getId().getUserId(), allRewardScores.getId().getCourseId());
        log.debug("Content worked on: {}", event.getContentId());
        log.debug("Content list: {}", contents);

        final RewardScoreEntity growthEntity = allRewardScores.getGrowth();
        final int oldScore = growthEntity.getValue();
        final int currentScore = calculateCurrentGrowth(contents);
        final int totalScore = getTotalAchievableGrowth(contents);

        log.debug("Old growth score: {}", oldScore);
        log.debug("Current growth score calculated: {}", currentScore);
        log.debug("Total achievable growth score: {}", totalScore);

        final int diff = currentScore - oldScore;

        if (diff == 0) {
            // no change in growth score, so no log entry is created
            return growthEntity;
        }

        final RewardScoreLogEntry logEntry = createLogEntry(oldScore, currentScore, event.getContentId());

        growthEntity.setValue(currentScore);
        growthEntity.setPercentage(calculatePercentage(currentScore, totalScore));
        growthEntity.getLog().add(logEntry);

        return growthEntity;
    }

    private static RewardScoreLogEntry createLogEntry(final int oldScore, final int currentScore, final UUID contentId) {
        return RewardScoreLogEntry.builder()
                .date(OffsetDateTime.now())
                .difference(currentScore - oldScore)
                .newValue(currentScore)
                .oldValue(oldScore)
                .reason(RewardChangeReason.CONTENT_DONE)
                .associatedContentIds(List.of(contentId))
                .build();
    }

    private float calculatePercentage(final int currentScore, final int totalScore) {
        if (totalScore == 0) {
            return 0;
        }
        return (float) currentScore / totalScore;
    }

    /**
     * Calculates the total achievable growth score of a course.
     *
     * @param contents the contents of the course
     * @return the total achievable growth score
     */
    private int getTotalAchievableGrowth(final List<Content> contents) {
        return contents.stream()
                .mapToInt(content -> content.getMetadata().getRewardPoints())
                .sum();
    }

    /**
     * Calculates the current growth score of a user.
     *
     * @param contents the contents of the course
     * @return the current growth score
     */
    private int calculateCurrentGrowth(final List<Content> contents) {
        return contents.stream()
                // only consider contents that are learned
                .filter(content -> content.getUserProgressData().getIsLearned())
                // sum up the reward points of all learned contents
                .mapToInt(content -> content.getMetadata().getRewardPoints())
                .sum();
    }

}
