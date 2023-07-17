package de.unistuttgart.iste.gits.reward.service.calculation;

import de.unistuttgart.iste.gits.common.event.UserProgressLogEvent;
import de.unistuttgart.iste.gits.generated.dto.*;
import de.unistuttgart.iste.gits.reward.persistence.dao.AllRewardScoresEntity;
import de.unistuttgart.iste.gits.reward.persistence.dao.RewardScoreEntity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GrowthScoreTest {

    private final GrowthScoreCalculator growthScoreCalculator = new GrowthScoreCalculator();


    @Test
    void testGrowthScoreCalculation() {
        //arrange
        UUID courseId = UUID.randomUUID();

        UUID userId = UUID.randomUUID();

        AllRewardScoresEntity allRewardScores = dummyAllRewardScoresBuilder(courseId, userId).build();

        Content content1 = dummyContent(10, true);
        Content content2 = dummyContent(10, false);
        Content content3 = dummyContent(20, true);


        List<Content> contentList = new ArrayList<>();
        contentList.add(content1);
        contentList.add(content2);
        contentList.add(content3);
        UserProgressLogEvent progressLogEvent1 = UserProgressLogEvent.builder().contentId(content1.getId()).build();

        //act
        RewardScoreEntity rewardScore = growthScoreCalculator.calculateOnContentWorkedOn(allRewardScores, contentList, progressLogEvent1);


        //assert
        Assertions.assertEquals(0.75f, rewardScore.getPercentage(), 0.0f);
        Assertions.assertEquals(30, rewardScore.getValue());

    }

    private static AllRewardScoresEntity.AllRewardScoresEntityBuilder dummyAllRewardScoresBuilder(UUID courseId, UUID userId) {
        return AllRewardScoresEntity.builder()
                .id(new AllRewardScoresEntity.PrimaryKey(courseId, userId))
                .health(initializeRewardScoreEntity(100))
                .strength(initializeRewardScoreEntity(0))
                .fitness(initializeRewardScoreEntity(100))
                .growth(initializeRewardScoreEntity(0))
                .power(initializeRewardScoreEntity(0));

    }

    private static Content dummyContent(int rewardPoints, boolean success) {
        ContentMetadata metadata = ContentMetadata.builder().setRewardPoints(rewardPoints).build();
        ProgressLogItem progressLogItem = ProgressLogItem.builder().setSuccess(success).build();
        UserProgressData progressData = UserProgressData.builder().setLog(List.of(progressLogItem)).build();

        return MediaContent.builder().setId(UUID.randomUUID()).setMetadata(metadata).setUserProgressData(progressData).build();
    }

    private static RewardScoreEntity initializeRewardScoreEntity(int initialValue) {
        RewardScoreEntity rewardScoreEntity = new RewardScoreEntity();
        rewardScoreEntity.setValue(initialValue);
        rewardScoreEntity.setLog(new ArrayList<>());
        return rewardScoreEntity;
    }
}
