package de.unistuttgart.iste.gits.reward.service.calculation;

import de.unistuttgart.iste.gits.common.event.UserProgressLogEvent;
import de.unistuttgart.iste.gits.generated.dto.Content;
import de.unistuttgart.iste.gits.reward.persistence.dao.AllRewardScoresEntity;
import de.unistuttgart.iste.gits.reward.persistence.dao.RewardScoreEntity;

import java.util.List;

public interface ScoreCalculator {

    /**
     * Recalculation that is done every night.
     *
     * @param allRewardScores all reward scores
     * @param contents        all contents of the course
     * @return the new reward score
     */
    RewardScoreEntity recalculateScore(AllRewardScoresEntity allRewardScores, List<Content> contents);

    /**
     * Calculation that is done when a user works on a content.
     *
     * @param allRewardScores all reward scores
     * @param contents        all contents of the course
     * @param event           the event that triggered the calculation
     * @return the new reward score
     */
    RewardScoreEntity calculateOnContentWorkedOn(AllRewardScoresEntity allRewardScores,
                                                 List<Content> contents,
                                                 UserProgressLogEvent event);
}
