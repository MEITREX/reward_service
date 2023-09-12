package de.unistuttgart.iste.gits.reward.service.calculation;

import de.unistuttgart.iste.gits.common.event.UserProgressLogEvent;
import de.unistuttgart.iste.gits.generated.dto.*;
import de.unistuttgart.iste.gits.reward.persistence.entity.*;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Calculates the growth score of a user, according the concept documented
 * <a href="https://gits-enpro.readthedocs.io/en/latest/dev-manuals/gamification/Scoring%20System.html#growth">here</a>.
 */
@Component
public class GrowthScoreCalculator implements ScoreCalculator {
    @Override
    public RewardScoreEntity recalculateScore(AllRewardScoresEntity allRewardScores, List<Content> contents) {
        return allRewardScores.getGrowth();
    }

    @Override
    public RewardScoreEntity calculateOnContentWorkedOn(AllRewardScoresEntity allRewardScores, List<Content> contents, UserProgressLogEvent event) {
        RewardScoreEntity growthScore = allRewardScores.getGrowth();
        int oldScore = growthScore.getValue();
        int currentScore = getCurrentScore(contents);
        int totalScore = getTotalScore(contents);

        int diff = currentScore - oldScore;

        if (diff == 0) {
            // no change in growth score, so no log entry is created
            return growthScore;
        }

        RewardScoreLogEntry logEntry = RewardScoreLogEntry.builder()
                .date(OffsetDateTime.now())
                .difference(diff)
                .newValue(currentScore)
                .oldValue(oldScore)
                .reason(RewardChangeReason.CONTENT_DONE)
                .associatedContentIds(List.of(event.getContentId()))
                .build();


        growthScore.setValue(currentScore);
        growthScore.setPercentage(calculatePercentage(currentScore, totalScore));
        growthScore.getLog().add(logEntry);

        return growthScore;
    }

    private float calculatePercentage(int currentScore, int totalScore) {
        return (float) currentScore / totalScore;
    }

    private int getTotalScore(List<Content> contents) {
        return contents.stream()
                .mapToInt(content -> content.getMetadata().getRewardPoints())
                .sum();
    }

    private int getCurrentScore(List<Content> contents) {
        return contents.stream()
                .filter(content -> contentCompletedSuccessfully(content.getUserProgressData()))
                .mapToInt(content -> content.getMetadata().getRewardPoints())
                .sum();
    }

    private boolean contentCompletedSuccessfully(UserProgressData userProgressData) {
        return userProgressData.getLog().stream().anyMatch(ProgressLogItem::getSuccess);
    }
}
