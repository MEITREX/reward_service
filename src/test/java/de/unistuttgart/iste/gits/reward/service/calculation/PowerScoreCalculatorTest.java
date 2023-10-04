package de.unistuttgart.iste.gits.reward.service.calculation;

import de.unistuttgart.iste.gits.common.event.UserProgressLogEvent;
import de.unistuttgart.iste.gits.generated.dto.RewardChangeReason;
import de.unistuttgart.iste.gits.reward.persistence.entity.*;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

/**
 * Test class for {@link PowerScoreCalculator}
 */
class PowerScoreCalculatorTest {

    private final PowerScoreCalculator powerScoreCalculator = new PowerScoreCalculator();

    /**
     * Given a content exists
     * When calculateOnContentWorkedOn is called
     * Then the growth score is calculated depending on the other scores
     */
    @Test
    void calculateOnContentWorkedOnPowerScore() {
        final UserProgressLogEvent event = UserProgressLogEvent.builder()
                .userId(UUID.randomUUID())
                .contentId(UUID.randomUUID())
                .correctness(1)
                .hintsUsed(0)
                .success(true)
                .build();

        final AllRewardScoresEntity rewardScoresEntity = createAllRewardScoresEntityWithPower(0);

        final RewardScoreEntity power = powerScoreCalculator.calculateOnContentWorkedOn(rewardScoresEntity, List.of(), event);

        assertThat(power.getValue(), is(24));
    }

    /**
     * Given Fitness decreases to 0
     * When recalculateScore is called
     * Then the power score is decreased by the correct value and a log entry is added
     */
    @Test
    void powerDecreasesWhenFitnessDecreases() {
        final AllRewardScoresEntity rewardScoresEntity = createAllRewardScoresEntityWithPower(24);
        rewardScoresEntity.setFitness(initializeRewardScoreEntity(0));

        final RewardScoreEntity power = powerScoreCalculator.recalculateScore(rewardScoresEntity, List.of());


        assertThat(power.getValue(), is(22));
        assertThat(power.getLog(), hasSize(1));

        final RewardScoreLogEntry logEntry = power.getLog().get(0);
        assertThat(logEntry.getDifference(), is(-2));
        assertThat(logEntry.getOldValue(), is(24));
        assertThat(logEntry.getNewValue(), is(22));
        assertThat(logEntry.getReason(), is(RewardChangeReason.COMPOSITE_VALUE));
    }

    /**
     * Given no change in power
     * When recalculateScore is called
     * Then no log entry is added
     */
    @Test
    void noLogEntryOnNoDifference() {
        final AllRewardScoresEntity rewardScoresEntity = createAllRewardScoresEntityWithPower(24);

        final RewardScoreEntity power = powerScoreCalculator.recalculateScore(rewardScoresEntity, List.of());

        assertThat(power.getValue(), is(24));
        assertThat(power.getLog(), hasSize(0));
    }

    private AllRewardScoresEntity createAllRewardScoresEntityWithPower(final int power) {
        return AllRewardScoresEntity.builder()
                .health(RewardScoreEntity.builder().value(100).build())
                .fitness(RewardScoreEntity.builder().value(100).build())
                .growth(RewardScoreEntity.builder().value(10).build())
                .strength(RewardScoreEntity.builder().value(10).build())
                .power(RewardScoreEntity.builder().value(power).build())
                .build();
    }

    private static RewardScoreEntity initializeRewardScoreEntity(final int value) {
        final RewardScoreEntity rewardScoreEntity = new RewardScoreEntity();
        rewardScoreEntity.setValue(value);
        rewardScoreEntity.setLog(new ArrayList<>());
        return rewardScoreEntity;
    }
}
