package de.unistuttgart.iste.gits.reward.controller;

import de.unistuttgart.iste.gits.common.user_handling.LoggedInUser;
import de.unistuttgart.iste.gits.common.user_handling.UserCourseAccessValidator;
import de.unistuttgart.iste.gits.generated.dto.RewardScores;
import de.unistuttgart.iste.gits.generated.dto.ScoreboardItem;
import de.unistuttgart.iste.gits.reward.service.RewardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.ContextValue;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.UUID;

@Slf4j
@Controller
@RequiredArgsConstructor
public class RewardController {

    private final RewardService rewardService;

    @QueryMapping
    public RewardScores userCourseRewardScores(@Argument final UUID courseId, @ContextValue final LoggedInUser currentUser) {
        UserCourseAccessValidator.validateUserHasAccessToCourse(currentUser, LoggedInUser.UserRoleInCourse.STUDENT, courseId);
        return rewardService.getRewardScores(courseId, currentUser.getId());
    }

    @QueryMapping
    public RewardScores courseRewardScoresForUser(@Argument final UUID courseId, @Argument final UUID userId, @ContextValue final LoggedInUser currentUser) {
        UserCourseAccessValidator.validateUserHasAccessToCourse(currentUser, LoggedInUser.UserRoleInCourse.ADMINISTRATOR, courseId);
        return rewardService.getRewardScores(courseId, userId);
    }

    @QueryMapping
    public List<ScoreboardItem> scoreboard(@Argument final UUID courseId, @ContextValue final LoggedInUser currentUser) {
        UserCourseAccessValidator.validateUserHasAccessToCourse(currentUser, LoggedInUser.UserRoleInCourse.STUDENT, courseId);
        return rewardService.getScoreboard(courseId);
    }

    @MutationMapping
    public RewardScores recalculateScores(@Argument final UUID courseId, @Argument final UUID userId, @ContextValue final LoggedInUser currentUser) {
        UserCourseAccessValidator.validateUserHasAccessToCourse(currentUser, LoggedInUser.UserRoleInCourse.ADMINISTRATOR, courseId);
        return rewardService.recalculateScores(courseId, userId);
    }
}
