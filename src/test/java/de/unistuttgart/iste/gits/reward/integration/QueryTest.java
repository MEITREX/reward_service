package de.unistuttgart.iste.gits.reward.integration;

import de.unistuttgart.iste.gits.common.testutil.GraphQlApiTest;
import de.unistuttgart.iste.gits.generated.dto.ScoreboardItem;
import de.unistuttgart.iste.gits.reward.persistence.entity.AllRewardScoresEntity;
import de.unistuttgart.iste.gits.reward.persistence.entity.RewardScoreEntity;
import de.unistuttgart.iste.gits.reward.persistence.repository.AllRewardScoresRepository;
import de.unistuttgart.iste.gits.reward.service.CourseServiceClient;
import de.unistuttgart.iste.gits.reward.service.RewardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.test.tester.GraphQlTester;

import java.util.ArrayList;
import java.util.UUID;

@GraphQlApiTest
class QueryTest {

    @Autowired
    AllRewardScoresRepository allRewardScoresRepository;

    @Autowired
    CourseServiceClient courseServiceClient;

    @Autowired
    RewardService rewardService;




    /**
     * Given a user with a rewardScore exist
     * When the rewardScore is queried
     * Then the rewardScore is returned
     */
    @Test
    void testCourseRewardScoresForUser(GraphQlTester tester) {
        UUID courseId = UUID.randomUUID();
        UUID user = UUID.randomUUID();

        allRewardScoresRepository.save(AllRewardScoresEntity.builder()
                .id(new AllRewardScoresEntity.PrimaryKey(courseId, user))
                .health(initializeRewardScoreEntity(100))
                .strength(initializeRewardScoreEntity(0))
                .fitness(initializeRewardScoreEntity(100))
                .growth(initializeRewardScoreEntity(0))
                .power(initializeRewardScoreEntity(0))
                .build());

        String query = """
                 query($courseId: UUID!, $userId: UUID!) {
                    courseRewardScoresForUser(courseId: $courseId, userId: $userId) {
                        health {
                            value
                        }
                        fitness {
                            value
                        }
                        growth {
                            value
                        }
                        strength {
                            value
                        }
                        power {
                            value
                        }
                    
                    }
                }
                """;

        tester.document(query)
                .variable("courseId", courseId)
                .variable("userId", user)
                .execute()
                .path("courseRewardScoresForUser.health.value").entity(Integer.class).isEqualTo(100)
                .path("courseRewardScoresForUser.fitness.value").entity(Integer.class).isEqualTo(100)
                .path("courseRewardScoresForUser.growth.value").entity(Integer.class).isEqualTo(0)
                .path("courseRewardScoresForUser.strength.value").entity(Integer.class).isEqualTo(0)
                .path("courseRewardScoresForUser.power.value").entity(Integer.class).isEqualTo(0);
    }

    /**
     * Given two rewardScores exist
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
