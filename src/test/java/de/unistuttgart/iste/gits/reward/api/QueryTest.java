package de.unistuttgart.iste.gits.reward.api;

import de.unistuttgart.iste.gits.common.testutil.GraphQlApiTest;
import de.unistuttgart.iste.gits.common.testutil.InjectCurrentUserHeader;
import de.unistuttgart.iste.gits.common.user_handling.LoggedInUser;
import de.unistuttgart.iste.gits.generated.dto.ScoreboardItem;
import de.unistuttgart.iste.gits.reward.persistence.entity.AllRewardScoresEntity;
import de.unistuttgart.iste.gits.reward.persistence.entity.RewardScoreEntity;
import de.unistuttgart.iste.gits.reward.persistence.repository.AllRewardScoresRepository;
import de.unistuttgart.iste.gits.reward.service.RewardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.test.tester.GraphQlTester;

import java.util.ArrayList;
import java.util.UUID;

import static de.unistuttgart.iste.gits.common.testutil.TestUsers.userWithMembershipInCourseWithId;

@GraphQlApiTest
class QueryTest {

    @Autowired
    AllRewardScoresRepository allRewardScoresRepository;

    @Autowired
    RewardService rewardService;

    private final UUID courseId = UUID.randomUUID();

    @InjectCurrentUserHeader
    private final LoggedInUser loggedInUser = userWithMembershipInCourseWithId(courseId, LoggedInUser.UserRoleInCourse.ADMINISTRATOR);


    /**
     * Given a user with a rewardScore exist
     * When the rewardScore is queried
     * Then the rewardScore is returned
     */
    @Test
    void testCourseRewardScoresForUser(final GraphQlTester tester) {

        allRewardScoresRepository.save(AllRewardScoresEntity.builder()
                .id(new AllRewardScoresEntity.PrimaryKey(courseId, loggedInUser.getId()))
                .health(initializeRewardScoreEntity(100))
                .strength(initializeRewardScoreEntity(0))
                .fitness(initializeRewardScoreEntity(100))
                .growth(initializeRewardScoreEntity(0))
                .power(initializeRewardScoreEntity(0))
                .build());

        final String query = """
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
                .variable("userId", loggedInUser.getId())
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
    void testGetScoreboard(final GraphQlTester tester) {
        final UUID user1 = UUID.randomUUID();
        final UUID user2 = UUID.randomUUID();

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

        final var scoreboardItem1 = new ScoreboardItem(user1, 0);
        final var scoreboardItem2 = new ScoreboardItem(user2, 30);

        final String query = """
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

    private static RewardScoreEntity initializeRewardScoreEntity(final int initialValue) {
        final RewardScoreEntity rewardScoreEntity = new RewardScoreEntity();
        rewardScoreEntity.setValue(initialValue);
        rewardScoreEntity.setLog(new ArrayList<>());
        return rewardScoreEntity;
    }
}
