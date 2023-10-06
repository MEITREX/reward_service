package de.unistuttgart.iste.gits.reward.service;

import de.unistuttgart.iste.gits.common.event.*;
import de.unistuttgart.iste.gits.common.exception.IncompleteEventMessageException;
import de.unistuttgart.iste.gits.generated.dto.*;
import de.unistuttgart.iste.gits.reward.persistence.entity.AllRewardScoresEntity;
import de.unistuttgart.iste.gits.reward.persistence.entity.RewardScoreEntity;
import de.unistuttgart.iste.gits.reward.persistence.mapper.RewardScoreMapper;
import de.unistuttgart.iste.gits.reward.persistence.repository.AllRewardScoresRepository;
import de.unistuttgart.iste.gits.reward.service.calculation.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;

import static java.util.Comparator.comparing;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class RewardService {

    /**
     * The initial reward score values for a new entity, for the relative reward scores Health and Fitness.
     */
    private static final int INITIAL_RELATIVE_REWARD_SCORE = 100;
    /**
     * The initial reward score values for a new entity, for the absolute reward scores Strength, Growth and Power.
     */
    private static final int INITIAL_ABSOLUTE_REWARD_SCORE = 0;

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
    public RewardScores recalculateScores(final UUID courseId, final UUID userId) {

        final AllRewardScoresEntity allRewardScoresEntity = rewardScoresRepository
                .findById(new AllRewardScoresEntity.PrimaryKey(courseId, userId))
                .orElseGet(() -> initializeRewardScores(courseId, userId));

        final List<UUID> chapterIds = courseServiceClient.getChapterIds(courseId);

        final List<Content> contents = contentServiceClient.getContentsWithUserProgressData(userId, chapterIds);

        try {
            recalculateScoresAndUpdateEntity(allRewardScoresEntity, contents);
        } catch (final Exception e) {
            throw new RewardScoreCalculationException("Could not recalculate reward scores.", e);
        }

        final var result = rewardScoresRepository.save(allRewardScoresEntity);

        return mapper.entityToDto(result);
    }

    private void recalculateScoresAndUpdateEntity(final AllRewardScoresEntity allRewardScoresEntity,
                                                  final List<Content> contents) {
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

        final List<AllRewardScoresEntity> allRewardScoresEntities = rewardScoresRepository.findAll();
        for (final AllRewardScoresEntity allRewardScoresEntity : allRewardScoresEntities) {
            try {
                recalculateScores(allRewardScoresEntity.getId().getCourseId(), allRewardScoresEntity.getId().getUserId());
            } catch (final Exception e) {
                log.error("Could not recalculate reward scores for user {} in course {}",
                        allRewardScoresEntity.getId().getUserId(), allRewardScoresEntity.getId().getCourseId(), e);
            }
        }
        log.info("Recalculated reward scores for {} users", allRewardScoresEntities.size());
    }

    /**
     * Gets all reward scores for a given user and course and initializes them if they do not exist yet.
     *
     * @param courseId the id of the course
     * @param userId   the id of the user
     * @return the reward scores
     */
    public AllRewardScoresEntity getAllRewardScoresEntity(final UUID courseId, final UUID userId) {
        return rewardScoresRepository
                .findById(new AllRewardScoresEntity.PrimaryKey(courseId, userId))
                .orElseGet(() -> initializeRewardScores(courseId, userId));
    }

    /**
     * Gets all reward scores for a given user and course and initializes them if they do not exist yet.
     *
     * @param courseId the id of the course
     * @param userId   the id of the user
     * @return the reward scores
     */
    public RewardScores getRewardScores(final UUID courseId, final UUID userId) {
        final AllRewardScoresEntity allRewardScoresEntity = getAllRewardScoresEntity(courseId, userId);
        return mapper.entityToDto(allRewardScoresEntity);
    }

    /**
     * Calculates the new reward scores for a given user and course when the user works on a content.
     *
     * @param event the event that triggered the calculation
     * @return the new reward scores
     */
    public RewardScores calculateScoresOnContentWorkedOn(final UserProgressLogEvent event) {
        final UUID courseId = courseServiceClient.getCourseIdForContent(event.getContentId());

        AllRewardScoresEntity allRewardScoresEntity = getAllRewardScoresEntity(courseId, event.getUserId());

        final List<UUID> chapterIds = courseServiceClient.getChapterIds(courseId);
        final List<Content> contents
                = contentServiceClient.getContentsWithUserProgressData(event.getUserId(), chapterIds);

        try {
            calculateNewScoresOnContentWorkedOn(event, allRewardScoresEntity, contents);
        } catch (final Exception e) {
            throw new RewardScoreCalculationException("Error while calculating fitness score", e);
        }

        allRewardScoresEntity = rewardScoresRepository.save(allRewardScoresEntity);

        return mapper.entityToDto(allRewardScoresEntity);
    }

    private void calculateNewScoresOnContentWorkedOn(final UserProgressLogEvent event,
                                                     final AllRewardScoresEntity allRewardScoresEntity,
                                                     final List<Content> contents) {
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
    }

    /**
     * Initializes a new {@link AllRewardScoresEntity} for a given user and course
     * with the default values for the reward scores.
     *
     * @param courseId the id of the course
     * @param userId   the id of the user
     * @return the initialized entity
     */
    public AllRewardScoresEntity initializeRewardScores(final UUID courseId, final UUID userId) {
        final AllRewardScoresEntity allRewardScores = new AllRewardScoresEntity();
        allRewardScores.setId(new AllRewardScoresEntity.PrimaryKey(courseId, userId));

        initializeHealth(courseId, userId, allRewardScores);
        allRewardScores.setStrength(initializeRewardScoreEntity(INITIAL_ABSOLUTE_REWARD_SCORE));
        allRewardScores.setFitness(initializeRewardScoreEntity(INITIAL_RELATIVE_REWARD_SCORE));
        allRewardScores.setGrowth(initializeRewardScoreEntity(INITIAL_ABSOLUTE_REWARD_SCORE));
        allRewardScores.setPower(initializeRewardScoreEntity(INITIAL_ABSOLUTE_REWARD_SCORE));
        return rewardScoresRepository.save(allRewardScores);
    }

    /**
     * Initializes the health score for a new entity.
     * The health score will not be 100 if the course contains content that is due.
     *
     * @param courseId              the id of the course
     * @param userId                the id of the user
     * @param allRewardScoresEntity the entity to initialize
     */
    private void initializeHealth(final UUID courseId, final UUID userId, final AllRewardScoresEntity allRewardScoresEntity) {
        allRewardScoresEntity.setHealth(initializeRewardScoreEntity(INITIAL_RELATIVE_REWARD_SCORE));

        /*try {
            final List<UUID> chapterIds = courseServiceClient.getChapterIds(courseId);
            final List<Content> contents = contentServiceClient.getContentsWithUserProgressData(userId, chapterIds);
            // Calculate the initial health value for the new entity
            final int initialHealthValue = healthScoreCalculator.calculateInitialHealthValueForNewEntity(contents);
            allRewardScoresEntity.setHealth(initializeRewardScoreEntity(initialHealthValue));
        } catch (final CourseServiceClient.CourseServiceConnectionException |
                       ContentServiceClient.ContentServiceConnectionException e) {
            // Handle exceptions by falling back to default values
            allRewardScoresEntity.setHealth(initializeRewardScoreEntity(INITIAL_RELATIVE_REWARD_SCORE));
            //  log the exception for debugging or further analysis
            log.error("An error occurred while initializing reward scores:", e);
        }*/
    }

    /**
     * Initializes a reward score entity with the given initial value and an empty log.
     *
     * @param initialValue the initial value
     * @return the initialized entity
     */
    public RewardScoreEntity initializeRewardScoreEntity(final int initialValue) {
        final RewardScoreEntity rewardScoreEntity = new RewardScoreEntity();
        rewardScoreEntity.setValue(initialValue);
        rewardScoreEntity.setLog(new ArrayList<>());
        return rewardScoreEntity;
    }


    /**
     * Returns the scoreboard for a specific course. Sorted by power from highest to lowest.
     *
     * @param courseId of the course for which the scoreboard should be retrieved
     * @return scoreboard
     */
    public List<ScoreboardItem> getScoreboard(final UUID courseId) {
        final List<AllRewardScoresEntity> rewardScoresOfCourse = rewardScoresRepository.findAllRewardScoresEntitiesById_CourseId(courseId);

        return rewardScoresOfCourse.stream()
                .map(RewardService::createScoreboardItemFromRewardScores)
                // sort by power score from highest to lowest
                .sorted(comparing(ScoreboardItem::getPowerScore).reversed())
                .toList();
    }

    private static ScoreboardItem createScoreboardItemFromRewardScores(final AllRewardScoresEntity rewardScoreEntity) {
        return new ScoreboardItem(rewardScoreEntity.getId().getUserId(), rewardScoreEntity.getPower().getValue());
    }


    /**
     * Method that receives Course Change Event and handles DELETE events.
     * All reward data is then deleted that is connected to deleted course
     *
     * @param changeEvent a Course Change Event received over dapr
     * @throws IncompleteEventMessageException if the received message is incomplete
     */
    public void removeRewardData(final CourseChangeEvent changeEvent) throws IncompleteEventMessageException {

        // evaluate course Update message
        if (changeEvent.getCourseId() == null || changeEvent.getOperation() == null) {
            throw new IncompleteEventMessageException("Incomplete message received: all fields of a message must be non-null");
        }
        // only consider DELETE events
        if (changeEvent.getOperation() != CrudOperation.DELETE) {
            return;
        }

        final List<AllRewardScoresEntity> entitiesToBeDeleted = rewardScoresRepository.findAllRewardScoresEntitiesById_CourseId(changeEvent.getCourseId());

        rewardScoresRepository.deleteAllInBatch(entitiesToBeDeleted);

    }
}