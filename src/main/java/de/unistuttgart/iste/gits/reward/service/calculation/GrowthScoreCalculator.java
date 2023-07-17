package de.unistuttgart.iste.gits.reward.service.calculation;

import de.unistuttgart.iste.gits.common.event.UserProgressLogEvent;
import de.unistuttgart.iste.gits.generated.dto.Content;
import de.unistuttgart.iste.gits.generated.dto.ProgressLogItem;
import de.unistuttgart.iste.gits.generated.dto.RewardChangeReason;
import de.unistuttgart.iste.gits.generated.dto.UserProgressData;
import de.unistuttgart.iste.gits.reward.persistence.dao.AllRewardScoresEntity;
import de.unistuttgart.iste.gits.reward.persistence.dao.RewardScoreEntity;
import de.unistuttgart.iste.gits.reward.persistence.dao.RewardScoreLogEntry;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Component
public class GrowthScoreCalculator implements ScoreCalculator {
    @Override
    public RewardScoreEntity recalculateScore(AllRewardScoresEntity allRewardScores, List<Content> contents) {
        RewardScoreEntity growthScore = allRewardScores.getGrowth();

        return growthScore;
    }

    @Override
    public RewardScoreEntity calculateOnContentWorkedOn(AllRewardScoresEntity allRewardScores, List<Content> contents, UserProgressLogEvent event) {
        RewardScoreEntity growthScore = allRewardScores.getGrowth();
        int oldScore = growthScore.getValue();
        int currentScore = getCurrentScore(contents);
        int totalScore = getTotalScore(contents);

        int diff = currentScore - oldScore;

        if (diff == 0) {
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
