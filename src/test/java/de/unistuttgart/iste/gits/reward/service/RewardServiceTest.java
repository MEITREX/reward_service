package de.unistuttgart.iste.gits.reward.service;

import de.unistuttgart.iste.gits.generated.dto.ScoreboardItem;
import de.unistuttgart.iste.gits.reward.persistence.dao.AllRewardScoresEntity;
import de.unistuttgart.iste.gits.reward.persistence.dao.RewardScoreEntity;
import de.unistuttgart.iste.gits.reward.persistence.mapper.RewardScoreMapper;
import de.unistuttgart.iste.gits.reward.persistence.repository.AllRewardScoresRepository;
import de.unistuttgart.iste.gits.reward.service.calculation.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;

public class RewardServiceTest {

    private final AllRewardScoresRepository allRewardScoresRepository = mock(AllRewardScoresRepository.class);
    private final RewardScoreMapper rewardScoreMapper = mock(RewardScoreMapper.class);
    private final CourseServiceClient courseServiceClient = mock(CourseServiceClient.class);
    private final ContentServiceClient contentServiceClient = mock(ContentServiceClient.class);
    private final HealthScoreCalculator healthScoreCalculator = mock(HealthScoreCalculator.class);
    private final FitnessScoreCalculator fitnessScoreCalculator = mock(FitnessScoreCalculator.class);
    private final StrengthScoreCalculator strengthScoreCalculator = mock(StrengthScoreCalculator.class);
    private final PowerScoreCalculator powerScoreCalculator = mock(PowerScoreCalculator.class);
    private final GrowthScoreCalculator growthScoreCalculator = mock(GrowthScoreCalculator.class);


    private final RewardService rewardService = new RewardService(
            allRewardScoresRepository,
            rewardScoreMapper,
            courseServiceClient,
            contentServiceClient,
            healthScoreCalculator,
            fitnessScoreCalculator,
            strengthScoreCalculator,
            powerScoreCalculator,
            growthScoreCalculator);

    /**
     * Given courseId
     * When getScoreboard is called
     * Then the scoreboard for the course is returned
     */
    @Test
    void testGetScoreboardSuccessfully() {
        // arrange test data
        UUID courseId = UUID.randomUUID();

        UUID userId1 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();
        UUID userId3 = UUID.randomUUID();

        AllRewardScoresEntity rewardScores1 = dummyAllRewardScoresBuilder(courseId, userId1).power(initializeRewardScoreEntity(10)).build();
        AllRewardScoresEntity rewardScores2 = dummyAllRewardScoresBuilder(courseId, userId2).power(initializeRewardScoreEntity(30)).build();
        AllRewardScoresEntity rewardScores3 = dummyAllRewardScoresBuilder(courseId, userId3).build();

        List<AllRewardScoresEntity> rewardScoresEntities = new ArrayList<>();
        rewardScoresEntities.add(rewardScores1);
        rewardScoresEntities.add(rewardScores2);
        rewardScoresEntities.add(rewardScores3);

        // mock repository
        when(allRewardScoresRepository.findAllRewardScoresEntitiesById_CourseId(courseId)).thenReturn(rewardScoresEntities);

        // act
        List<ScoreboardItem> scoreboardItemList = rewardService.getScoreboard(courseId);

        //assert
        assertThat(scoreboardItemList.size(), is(3));
        assertThat(scoreboardItemList.get(0).getUserId(), is(rewardScores2.getId().getUserId()));
        assertThat(scoreboardItemList.get(1).getUserId(), is(rewardScores1.getId().getUserId()));
        assertThat(scoreboardItemList.get(2).getUserId(), is(rewardScores3.getId().getUserId()));

        // verify that the repository was called
        verify(allRewardScoresRepository, times(1)).findAllRewardScoresEntitiesById_CourseId(courseId);


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

    private static RewardScoreEntity initializeRewardScoreEntity(int initialValue) {
        RewardScoreEntity rewardScoreEntity = new RewardScoreEntity();
        rewardScoreEntity.setValue(initialValue);
        rewardScoreEntity.setLog(new ArrayList<>());
        return rewardScoreEntity;
    }


}

