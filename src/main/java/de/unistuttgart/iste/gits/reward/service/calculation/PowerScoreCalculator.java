package de.unistuttgart.iste.gits.reward.service.calculation;

import de.unistuttgart.iste.gits.common.event.UserProgressLogEvent;
import de.unistuttgart.iste.gits.generated.dto.Content;
import de.unistuttgart.iste.gits.reward.persistence.dao.AllRewardScoresEntity;
import de.unistuttgart.iste.gits.reward.persistence.dao.RewardScoreEntity;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PowerScoreCalculator implements ScoreCalculator {


    @Override
    public RewardScoreEntity recalculateScore(AllRewardScoresEntity allRewardScores, List<Content> contents) {
        RewardScoreEntity powerScoreBefore = allRewardScores.getPower();

        // TODO calculate power score

        return powerScoreBefore;
    }

    @Override
    public RewardScoreEntity calculateOnContentWorkedOn(AllRewardScoresEntity allRewardScores, List<Content> contents, UserProgressLogEvent event) {
        RewardScoreEntity powerScoreBefore = allRewardScores.getPower();

        // TODO calculate power score

        return powerScoreBefore;
    }
}
