package de.unistuttgart.iste.gits.reward.api;

import de.unistuttgart.iste.gits.common.testutil.*;
import de.unistuttgart.iste.gits.common.user_handling.LoggedInUser;
import de.unistuttgart.iste.gits.common.user_handling.LoggedInUser.UserRoleInCourse;
import org.junit.jupiter.api.Test;
import org.springframework.graphql.test.tester.GraphQlTester;

import java.util.UUID;

import static de.unistuttgart.iste.gits.common.testutil.TestUsers.userWithMembershipInCourseWithId;

@GraphQlApiTest
class TestAuthorization {

    private final UUID courseId = UUID.randomUUID();

    @InjectCurrentUserHeader
    private final LoggedInUser loggedInUser = userWithMembershipInCourseWithId(courseId, UserRoleInCourse.STUDENT);

    @Test
    void testRecalculateScoresOnlyForAdmins(final GraphQlTester tester) {
        final String query = """
                mutation($courseId: UUID!, $userId: UUID!) {
                    recalculateScores(courseId: $courseId, userId: $userId) {
                        health {
                            value
                        }
                    }
                }
                """;

        tester.document(query)
                .variable("courseId", courseId)
                .variable("userId", loggedInUser.getId())
                .execute()
                .errors()
                .satisfy(AuthorizationAsserts::assertIsMissingUserRoleError);
    }

    @Test
    void testUserSpecificScoresScoresOnlyForAdmins(final GraphQlTester tester) {
        final String query = """
                query ($courseId: UUID!, $userId: UUID!) {
                    courseRewardScoresForUser(courseId: $courseId, userId: $userId) {
                        health {
                            value
                        }
                    }
                }
                """;

        tester.document(query)
                .variable("courseId", courseId)
                .variable("userId", UUID.randomUUID())
                .execute()
                .errors()
                .satisfy(AuthorizationAsserts::assertIsMissingUserRoleError);
    }
}
