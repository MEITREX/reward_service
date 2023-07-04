package de.unistuttgart.iste.gits.reward.service;

import de.unistuttgart.iste.gits.common.event.UserProgressLogEvent;
import de.unistuttgart.iste.gits.generated.dto.Content;
import de.unistuttgart.iste.gits.generated.dto.RewardScores;
import de.unistuttgart.iste.gits.reward.persistence.dao.AllRewardScoresEntity;
import de.unistuttgart.iste.gits.reward.persistence.dao.RewardScoreEntity;
import de.unistuttgart.iste.gits.reward.persistence.mapper.RewardScoreMapper;
import de.unistuttgart.iste.gits.reward.persistence.repository.AllRewardScoresRepository;
import de.unistuttgart.iste.gits.reward.service.calculation.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class RewardService {

    private final AllRewardScoresRepository rewardScoresRepository;
    private final RewardScoreMapper mapper;
    private final CourseServiceClient courseServiceClient;
    private final ContentServiceClient contentServiceClient;

    private final HealthScoreCalculator healthScoreCalculator;
    private final FitnessScoreCalculator fitnessScoreCalculator;
    private final StrengthScoreCalculator strengthScoreCalculator;
    private final PowerScoreCalculator powerScoreCalculator;
    private final GrowthScoreCalculator growthScoreCalculator;

    /**
     * Recalculates the reward scores for a given user and course.
     *
     * @param courseId the id of the course
     * @param userId   the id of the user
     * @return the recalculated reward scores
     */
    public RewardScores recalculateScores(UUID courseId, UUID userId) {
        AllRewardScoresEntity allRewardScoresEntity = rewardScoresRepository
                .findById(new AllRewardScoresEntity.PrimaryKey(courseId, userId))
                .orElseGet(() -> initializeRewardScores(courseId, userId));

        List<UUID> chapterIds = courseServiceClient.getChapterIds(courseId);

        List<Content> contents = contentServiceClient.getContentsWithUserProgressData(userId, chapterIds);

        allRewardScoresEntity
                .setHealth(healthScoreCalculator.recalculateScore(allRewardScoresEntity.getHealth(), contents));
        allRewardScoresEntity
                .setFitness(fitnessScoreCalculator.recalculateScore(allRewardScoresEntity.getFitness(), contents));
        allRewardScoresEntity
                .setStrength(strengthScoreCalculator.recalculateScore(allRewardScoresEntity.getStrength(), contents));
        allRewardScoresEntity
                .setPower(powerScoreCalculator.recalculateScore(allRewardScoresEntity.getPower(), contents));
        allRewardScoresEntity
                .setGrowth(growthScoreCalculator.recalculateScore(allRewardScoresEntity.getGrowth(), contents));

        var result = rewardScoresRepository.save(allRewardScoresEntity);

        return mapper.entityToDto(result);
    }

    /**
     * Recalculates the reward scores for all users and courses.
     * <p>
     * By default, this method is called every day at 03:00.
     * This can be changed in the application.properties file.
     */
    @Scheduled(cron = "${reward.recalculation.cron}")
    public void recalculateAllScores() {
        CourseServiceClient.clearCache();

        List<AllRewardScoresEntity> allRewardScoresEntities = rewardScoresRepository.findAll();
        for (AllRewardScoresEntity allRewardScoresEntity : allRewardScoresEntities) {
            try {
                recalculateScores(allRewardScoresEntity.getId().getCourseId(), allRewardScoresEntity.getId().getUserId());
            } catch (Exception e) {
                log.error("Could not recalculate reward scores for user {} in course {}",
                        allRewardScoresEntity.getId().getUserId(), allRewardScoresEntity.getId().getCourseId(), e);
            }
        }
        log.info("Recalculated reward scores for {} users", allRewardScoresEntities.size());
    }

    public AllRewardScoresEntity getAllRewardScoresEntity(UUID courseId, UUID userId) {
        return rewardScoresRepository
                .findById(new AllRewardScoresEntity.PrimaryKey(courseId, userId))
                .orElseGet(() -> initializeRewardScores(courseId, userId));
    }

    public RewardScores getRewardScores(UUID courseId, UUID userId) {
        return mapper.entityToDto(getAllRewardScoresEntity(courseId, userId));
    }

    public RewardScores calculateScoresOnContentWorkedOn(UserProgressLogEvent event) {
        UUID courseId = courseServiceClient.getCourseIdForContent(event.getContentId());

        AllRewardScoresEntity allRewardScoresEntity = rewardScoresRepository
                .findById(new AllRewardScoresEntity.PrimaryKey(courseId, event.getUserId()))
                .orElseGet(() -> initializeRewardScores(courseId, event.getUserId()));

        List<UUID> chapterIds = courseServiceClient.getChapterIds(courseId);
        List<Content> contents
                = contentServiceClient.getContentsWithUserProgressData(event.getUserId(), chapterIds);

        allRewardScoresEntity.setHealth(healthScoreCalculator
                .calculateOnContentWorkedOn(allRewardScoresEntity.getHealth(), contents, event));
        allRewardScoresEntity.setFitness(fitnessScoreCalculator
                .calculateOnContentWorkedOn(allRewardScoresEntity.getFitness(), contents, event));
        allRewardScoresEntity.setStrength(strengthScoreCalculator
                .calculateOnContentWorkedOn(allRewardScoresEntity.getStrength(), contents, event));
        allRewardScoresEntity.setPower(powerScoreCalculator
                .calculateOnContentWorkedOn(allRewardScoresEntity.getPower(), contents, event));
        allRewardScoresEntity.setGrowth(growthScoreCalculator
                .calculateOnContentWorkedOn(allRewardScoresEntity.getGrowth(), contents, event));

        allRewardScoresEntity = rewardScoresRepository.save(allRewardScoresEntity);

        return mapper.entityToDto(allRewardScoresEntity);
    }

    public AllRewardScoresEntity initializeRewardScores(UUID courseId, UUID userId) {
        AllRewardScoresEntity allRewardScoresEntity = new AllRewardScoresEntity();
        allRewardScoresEntity.setId(new AllRewardScoresEntity.PrimaryKey(courseId, userId));
        allRewardScoresEntity.setHealth(initializeRewardScoreEntity(100));
        allRewardScoresEntity.setStrength(initializeRewardScoreEntity(0));
        allRewardScoresEntity.setFitness(initializeRewardScoreEntity(100));
        allRewardScoresEntity.setGrowth(initializeRewardScoreEntity(0));
        allRewardScoresEntity.setPower(initializeRewardScoreEntity(0));
        return rewardScoresRepository.save(allRewardScoresEntity);
    }

    public RewardScoreEntity initializeRewardScoreEntity(int initialValue) {
        RewardScoreEntity rewardScoreEntity = new RewardScoreEntity();
        rewardScoreEntity.setValue(initialValue);
        rewardScoreEntity.setLog(new ArrayList<>());
        return rewardScoreEntity;
    }

}
