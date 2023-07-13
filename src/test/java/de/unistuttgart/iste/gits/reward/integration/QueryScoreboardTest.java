package de.unistuttgart.iste.gits.reward.integration;

import de.unistuttgart.iste.gits.common.testutil.GraphQlApiTest;
import de.unistuttgart.iste.gits.generated.dto.ScoreboardItem;
import de.unistuttgart.iste.gits.reward.persistence.dao.AllRewardScoresEntity;
import de.unistuttgart.iste.gits.reward.persistence.dao.RewardScoreEntity;
import de.unistuttgart.iste.gits.reward.persistence.repository.AllRewardScoresRepository;
import de.unistuttgart.iste.gits.reward.service.CourseServiceClient;
import de.unistuttgart.iste.gits.reward.service.RewardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.test.tester.GraphQlTester;

import java.util.ArrayList;
import java.util.UUID;

@GraphQlApiTest
public class QueryScoreboardTest {

    @Autowired
    AllRewardScoresRepository allRewardScoresRepository;

    @Autowired
    CourseServiceClient courseServiceClient;

    @Autowired
    RewardService rewardService;

    /**
     * Given two rewardscores exist
     * When the scoreboard is queried
     * Then the scoreboard is returned with the correct scores
     */
    @Test
    void testGetScoreboard(GraphQlTester tester) {
        UUID courseId = UUID.randomUUID();
        UUID user1 = UUID.randomUUID();
        UUID user2 = UUID.randomUUID();

        allRewardScoresRepository.save(AllRewardScoresEntity.builder()
                .id(new AllRewardScoresEntity.PrimaryKey(courseId, user1))
                .health(initializeRewardScoreEntity(100))
                .strength(initializeRewardScoreEntity(0))
                .fitness(initializeRewardScoreEntity(100))
                .growth(initializeRewardScoreEntity(0))
                .power(initializeRewardScoreEntity(0))
                .build());

        allRewardScoresRepository.save(AllRewardScoresEntity.builder()
                .id(new AllRewardScoresEntity.PrimaryKey(courseId, user2))
                .health(initializeRewardScoreEntity(100))
                .strength(initializeRewardScoreEntity(0))
                .fitness(initializeRewardScoreEntity(100))
                .growth(initializeRewardScoreEntity(0))
                .power(initializeRewardScoreEntity(30))
                .build());

        var scoreboardItem1 = new ScoreboardItem(user1, 0);
        var scoreboardItem2 = new ScoreboardItem(user2, 30);

        String query = """
                query($courseId: UUID!) {
                    scoreboard(courseId: $courseId) {
                            userId
                            powerScore
                    }
                }""";

        tester.document(query)
                .variable("courseId", courseId)
                .execute()
                .path("scoreboard").entityList(ScoreboardItem.class).hasSize(2)
                .contains(scoreboardItem2, scoreboardItem1);

    }

    private static RewardScoreEntity initializeRewardScoreEntity(int initialValue) {
        RewardScoreEntity rewardScoreEntity = new RewardScoreEntity();
        rewardScoreEntity.setValue(initialValue);
        rewardScoreEntity.setLog(new ArrayList<>());
        return rewardScoreEntity;
    }
}
