package de.unistuttgart.iste.gits.reward.service.calculation;

import de.unistuttgart.iste.gits.common.event.UserProgressLogEvent;
import de.unistuttgart.iste.gits.generated.dto.Content;
import de.unistuttgart.iste.gits.generated.dto.RewardChangeReason;
import de.unistuttgart.iste.gits.reward.persistence.entity.*;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

/**
 * Calculates the power score of a user, according the concept documented
 * <a href="https://gits-enpro.readthedocs.io/en/latest/dev-manuals/gamification/Scoring%20System.html#power">here</a>.
 */
@Component
public class PowerScoreCalculator implements ScoreCalculator {

    public static final double HEALTH_FITNESS_MULTIPLIER = 0.1;

    @Override
    public RewardScoreEntity recalculateScore(AllRewardScoresEntity allRewardScores, List<Content> contents) {
        return calculatePowerScore(allRewardScores);
    }

    @Override
    public RewardScoreEntity calculateOnContentWorkedOn(AllRewardScoresEntity allRewardScores, List<Content> contents, UserProgressLogEvent event) {
        return calculatePowerScore(allRewardScores);
    }

    private RewardScoreEntity calculatePowerScore(AllRewardScoresEntity allRewardScores) {
        int growth = allRewardScores.getGrowth().getValue();
        int strength = allRewardScores.getStrength().getValue();
        int health = allRewardScores.getHealth().getValue();
        int fitness = allRewardScores.getFitness().getValue();
        int power = allRewardScores.getPower().getValue();

        double powerValue = (growth + strength) + HEALTH_FITNESS_MULTIPLIER * 0.01 * (health + fitness) * (growth + strength);
        int powerRounded = (int) Math.round(powerValue);

        int difference = powerRounded - power;
        if (difference == 0) {
            // no change in power score, so no log entry is created
            return allRewardScores.getPower();
        }

        RewardScoreLogEntry logEntry = RewardScoreLogEntry.builder()
                .date(OffsetDateTime.now())
                .difference(difference)
                .oldValue(power)
                .newValue(powerRounded)
                .reason(RewardChangeReason.COMPOSITE_VALUE)
                .associatedContentIds(Collections.emptyList())
                .build();

        RewardScoreEntity powerEntity = allRewardScores.getPower();
        powerEntity.setValue(powerRounded);
        powerEntity.getLog().add(logEntry);

        return powerEntity;
    }
}
