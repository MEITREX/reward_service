package de.unistuttgart.iste.gits.reward.service.calculation;

import de.unistuttgart.iste.gits.common.event.UserProgressUpdatedEvent;
import de.unistuttgart.iste.gits.generated.dto.*;
import de.unistuttgart.iste.gits.reward.persistence.entity.AllRewardScoresEntity;
import de.unistuttgart.iste.gits.reward.persistence.entity.RewardScoreEntity;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GrowthScoreCalculatorTest {

    private final GrowthScoreCalculator growthScoreCalculator = new GrowthScoreCalculator();

    @Test
    void testGrowthScoreCalculation() {
        // arrange
        final UUID courseId = UUID.randomUUID();

        final UUID userId = UUID.randomUUID();

        final AllRewardScoresEntity allRewardScores = dummyAllRewardScoresBuilder(courseId, userId).build();

        final Content content1 = dummyContent(10, true);
        final Content content2 = dummyContent(10, false);
        final Content content3 = dummyContent(20, true);

        final List<Content> contentList = List.of(content1, content2, content3);
        final UserProgressUpdatedEvent progressLogEvent1 = UserProgressUpdatedEvent.builder()
                .contentId(content1.getId())
                .build();

        // act
        final RewardScoreEntity rewardScore = growthScoreCalculator.calculateOnContentWorkedOn(allRewardScores, contentList, progressLogEvent1);

        // assert
        assertEquals(0.75f, rewardScore.getPercentage(), 0.05f);
        assertEquals(30, rewardScore.getValue());

    }

    private static AllRewardScoresEntity.AllRewardScoresEntityBuilder dummyAllRewardScoresBuilder(final UUID courseId,
                                                                                                  final UUID userId) {
        return AllRewardScoresEntity.builder()
                .id(new AllRewardScoresEntity.PrimaryKey(courseId, userId))
                .health(initializeRewardScoreEntity(100))
                .strength(initializeRewardScoreEntity(0))
                .fitness(initializeRewardScoreEntity(100))
                .growth(initializeRewardScoreEntity(0))
                .power(initializeRewardScoreEntity(0));

    }

    private static Content dummyContent(final int rewardPoints, final boolean success) {
        final ContentMetadata metadata = ContentMetadata.builder()
                .setRewardPoints(rewardPoints)
                .build();
        final UserProgressData progressData = UserProgressData.builder()
                .setIsLearned(success)
                .setLog(List.of())
                .build();

        return MediaContent.builder().setId(UUID.randomUUID())
                .setMetadata(metadata)
                .setUserProgressData(progressData)
                .build();
    }

    private static RewardScoreEntity initializeRewardScoreEntity(final int initialValue) {
        final RewardScoreEntity rewardScoreEntity = new RewardScoreEntity();
        rewardScoreEntity.setValue(initialValue);
        rewardScoreEntity.setLog(new ArrayList<>());
        return rewardScoreEntity;
    }
}
