package de.unistuttgart.iste.gits.reward.service;

import de.unistuttgart.iste.gits.common.event.CourseChangeEvent;
import de.unistuttgart.iste.gits.common.event.CrudOperation;
import de.unistuttgart.iste.gits.common.event.UserProgressLogEvent;
import de.unistuttgart.iste.gits.generated.dto.*;
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

import java.util.*;

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

        try {
            allRewardScoresEntity
                    .setHealth(healthScoreCalculator.recalculateScore(allRewardScoresEntity, contents));
            allRewardScoresEntity
                    .setFitness(fitnessScoreCalculator.recalculateScore(allRewardScoresEntity, contents));
            allRewardScoresEntity
                    .setStrength(strengthScoreCalculator.recalculateScore(allRewardScoresEntity, contents));
            allRewardScoresEntity
                    .setGrowth(growthScoreCalculator.recalculateScore(allRewardScoresEntity, contents));
            allRewardScoresEntity
                    .setPower(powerScoreCalculator.recalculateScore(allRewardScoresEntity, contents));
        } catch (Exception e) {
            throw new RewardScoreCalculationException("Could not recalculate reward scores", e);
        }

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

        try {
            allRewardScoresEntity.setHealth(healthScoreCalculator
                    .calculateOnContentWorkedOn(allRewardScoresEntity, contents, event));
            allRewardScoresEntity.setFitness(fitnessScoreCalculator
                    .calculateOnContentWorkedOn(allRewardScoresEntity, contents, event));
            allRewardScoresEntity.setStrength(strengthScoreCalculator
                    .calculateOnContentWorkedOn(allRewardScoresEntity, contents, event));
            allRewardScoresEntity.setGrowth(growthScoreCalculator
                    .calculateOnContentWorkedOn(allRewardScoresEntity, contents, event));
            allRewardScoresEntity.setPower(powerScoreCalculator
                    .calculateOnContentWorkedOn(allRewardScoresEntity, contents, event));
        } catch (Exception e) {
            throw new RewardScoreCalculationException("Error while calculating fitness score", e);
        }

        allRewardScoresEntity = rewardScoresRepository.save(allRewardScoresEntity);

        return mapper.entityToDto(allRewardScoresEntity);
    }

    public AllRewardScoresEntity initializeRewardScores(UUID courseId, UUID userId) {
        AllRewardScoresEntity allRewardScoresEntity = new AllRewardScoresEntity();
        allRewardScoresEntity.setId(new AllRewardScoresEntity.PrimaryKey(courseId, userId));

        try {
            List<UUID> chapterIds = courseServiceClient.getChapterIds(courseId);
            List<Content> contents = contentServiceClient.getContentsWithUserProgressData(userId, chapterIds);
            // Calculate the initial health value for the new entity
            int initialHealthValue = healthScoreCalculator.calculateInitialHealthValueForNewEntity(contents);
            allRewardScoresEntity.setHealth(initializeRewardScoreEntity(initialHealthValue));
        } catch (CourseServiceClient.CourseServiceConnectionException |
                 ContentServiceClient.ContentServiceConnectionException e) {
            // Handle exceptions by falling back to default values
            allRewardScoresEntity.setHealth(initializeRewardScoreEntity(100));
            //  log the exception for debugging or further analysis
            log.error("An error occurred while initializing reward scores:", e);
        }
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


    /**
     * Returns the scoreboard for a specific course. Sorted by power from highest to lowest.
     * @param courseId of the course for which the scoreboard should be retrieved
     * @return scoreboard
     */
    public List<ScoreboardItem> getScoreboard(UUID courseId) {
        List<AllRewardScoresEntity> courseRewardScoreEntities = rewardScoresRepository.findAllRewardScoresEntitiesById_CourseId(courseId);
        List<ScoreboardItem> scoreboard = new ArrayList<>();

        for (var rewardScore : courseRewardScoreEntities) {
            ScoreboardItem scoreboardItem = new ScoreboardItem(rewardScore.getId().getUserId(), rewardScore.getPower().getValue());
            scoreboard.add(scoreboardItem);
        }

        return scoreboard.stream()
                .sorted(Comparator.comparing(ScoreboardItem::getPowerScore).reversed())
                .toList();
    }

    /**
     * Method that receives Course Change Event and handles DELETE events.
     * All reward data is then deleted that is connected to deleted course
     * @param changeEvent a Course Change Event received over dapr
     */
    public void removeRewardData(CourseChangeEvent changeEvent){

        // evaluate course Update message
        if (changeEvent.getCourseId() == null || changeEvent.getOperation() == null){
            throw new NullPointerException("incomplete message received: all fields of a message must be non-null");
        }
        //only consider DELETE events
        if (!changeEvent.getOperation().equals(CrudOperation.DELETE)){
            return;
        }

        List<AllRewardScoresEntity> entitiesToBeDeleted = rewardScoresRepository.findAllRewardScoresEntitiesById_CourseId(changeEvent.getCourseId());

        rewardScoresRepository.deleteAllInBatch(entitiesToBeDeleted);

    }
}