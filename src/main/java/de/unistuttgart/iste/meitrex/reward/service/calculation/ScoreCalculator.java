package de.unistuttgart.iste.meitrex.reward.service.calculation;

import de.unistuttgart.iste.meitrex.common.event.UserProgressUpdatedEvent;
import de.unistuttgart.iste.meitrex.generated.dto.Content;
import de.unistuttgart.iste.meitrex.reward.persistence.entity.AllRewardScoresEntity;
import de.unistuttgart.iste.meitrex.reward.persistence.entity.RewardScoreEntity;

import java.util.List;

/**
 * Common interface for all score calculators.
 */
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
                                                 UserProgressUpdatedEvent event);
}
