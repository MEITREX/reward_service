package de.unistuttgart.iste.gits.reward.service.calculation;

import de.unistuttgart.iste.gits.common.event.UserProgressLogEvent;
import de.unistuttgart.iste.gits.generated.dto.Content;
import de.unistuttgart.iste.gits.reward.persistence.entity.AllRewardScoresEntity;
import de.unistuttgart.iste.gits.reward.persistence.entity.RewardScoreEntity;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Calculates the strength score of a user, according the concept documented
 * <a href="https://gits-enpro.readthedocs.io/en/latest/dev-manuals/gamification/Scoring%20System.html#strength">here</a>.
 * <p>
 * Note: This class is not implemented yet and is not scope of this project.
 */
@Component
public class StrengthScoreCalculator implements ScoreCalculator {
    @Override
    public RewardScoreEntity recalculateScore(AllRewardScoresEntity allRewardScores, List<Content> contents) {
        return allRewardScores.getStrength();
    }

    @Override
    public RewardScoreEntity calculateOnContentWorkedOn(AllRewardScoresEntity allRewardScores, List<Content> contents, UserProgressLogEvent event) {
        return allRewardScores.getStrength();
    }
}
