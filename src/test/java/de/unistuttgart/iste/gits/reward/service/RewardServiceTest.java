package de.unistuttgart.iste.gits.reward.service;

import de.unistuttgart.iste.gits.common.event.*;
import de.unistuttgart.iste.gits.common.exception.IncompleteEventMessageException;
import de.unistuttgart.iste.gits.content_service.client.ContentServiceClient;
import de.unistuttgart.iste.gits.content_service.exception.ContentServiceConnectionException;
import de.unistuttgart.iste.gits.generated.dto.*;
import de.unistuttgart.iste.gits.reward.persistence.entity.AllRewardScoresEntity;
import de.unistuttgart.iste.gits.reward.persistence.entity.RewardScoreEntity;
import de.unistuttgart.iste.gits.reward.persistence.mapper.RewardScoreMapper;
import de.unistuttgart.iste.gits.reward.persistence.repository.AllRewardScoresRepository;
import de.unistuttgart.iste.gits.reward.service.calculation.*;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class RewardServiceTest {

    private final AllRewardScoresRepository allRewardScoresRepository = mock(AllRewardScoresRepository.class);
    private final RewardScoreMapper rewardScoreMapper = mock(RewardScoreMapper.class);
    private final HealthScoreCalculator healthScoreCalculator = mock(HealthScoreCalculator.class);
    private final FitnessScoreCalculator fitnessScoreCalculator = mock(FitnessScoreCalculator.class);
    private final StrengthScoreCalculator strengthScoreCalculator = mock(StrengthScoreCalculator.class);
    private final PowerScoreCalculator powerScoreCalculator = mock(PowerScoreCalculator.class);
    private final GrowthScoreCalculator growthScoreCalculator = mock(GrowthScoreCalculator.class);

    private final ContentServiceClient contentServiceClient = mock(ContentServiceClient.class);

    private final RewardService rewardService = new RewardService(
            allRewardScoresRepository,
            rewardScoreMapper,
            healthScoreCalculator,
            fitnessScoreCalculator,
            strengthScoreCalculator,
            powerScoreCalculator,
            growthScoreCalculator,
            contentServiceClient);

    /**
     * Given a courseId and userID
     * when initializeRewardScores is called
     * Then create a new allRewardScoresEntity with the default values
     */
    @Test
    void testInitializeRewardScoreEntity() {
        final UUID courseId = UUID.randomUUID();
        final UUID userId = UUID.randomUUID();

        final AllRewardScoresEntity expectedEntity = dummyAllRewardScoresBuilder(courseId, userId).build();

        when(allRewardScoresRepository.save(any())).thenReturn(expectedEntity);

        final AllRewardScoresEntity rewardScoresEntity = rewardService.initializeRewardScores(courseId, userId);

        verify(allRewardScoresRepository).save(any());
        assertThat(rewardScoresEntity, is(expectedEntity));

    }

    /**
     * Given a progressEvent
     * when calculateScoresOnContentWorkedOn is called
     * Then update the rewardScores and return it
     */
    @Test
    void testCalculateScoresOnContentWorkedOn() throws ContentServiceConnectionException {
        final UUID contentId = UUID.randomUUID();
        final UUID courseId = UUID.randomUUID();
        final UUID userID = UUID.randomUUID();

        final AllRewardScoresEntity.PrimaryKey primaryKey = new AllRewardScoresEntity.PrimaryKey(courseId, userID);

        final UserProgressData progressData = UserProgressData.builder().build();
        final List<Content> contents = List.of(createContentWithUserData(contentId, progressData));

        final UserProgressUpdatedEvent event = UserProgressUpdatedEvent.builder()
                .userId(userID)
                .contentId(contentId)
                .courseId(courseId)
                .chapterId(UUID.randomUUID())
                .correctness(1)
                .hintsUsed(0)
                .success(true)
                .build();

        final AllRewardScoresEntity allRewardScoresEntity = dummyAllRewardScoresBuilder(courseId, userID).build();

        final RewardScores expectedRewardScores = new RewardScores(
                new RewardScore(100, 0, null),
                new RewardScore(100, 0, null),
                new RewardScore(0, 0, null),
                new RewardScore(0, 0, null),
                new RewardScore(0, 0, null));

        when(allRewardScoresRepository.findById(primaryKey)).thenReturn(Optional.ofNullable(allRewardScoresEntity));
        when(allRewardScoresRepository.save(any())).thenReturn(allRewardScoresEntity);
        when(contentServiceClient.queryContentsOfCourse(userID, courseId)).thenReturn(contents);
        when(rewardScoreMapper.entityToDto(allRewardScoresEntity)).thenReturn(expectedRewardScores);

        final RewardScores rewardScores = rewardService.calculateScoresOnContentWorkedOn(event);

        assertThat(rewardScores, is(expectedRewardScores));
        verify(allRewardScoresRepository).save(any());
        verify(allRewardScoresRepository).findById(primaryKey);
        verify(contentServiceClient).queryContentsOfCourse(userID, courseId);
        verify(rewardScoreMapper).entityToDto(allRewardScoresEntity);

    }

    /**
     * Given a rewardScore
     * when recalculateScores is called
     * Then update the rewardScores and return it
     */
    @Test
    void testRecalculateScores() throws ContentServiceConnectionException {
        final UUID courseId = UUID.randomUUID();
        final UUID userId = UUID.randomUUID();
        final UUID contentId = UUID.randomUUID();
        final UUID chapterId1 = UUID.randomUUID();
        final UUID chapterId2 = UUID.randomUUID();

        final UserProgressData progressData = UserProgressData.builder().build();
        final AllRewardScoresEntity allRewardScoresEntity = dummyAllRewardScoresBuilder(courseId, userId).build();
        final AllRewardScoresEntity.PrimaryKey primaryKey = new AllRewardScoresEntity.PrimaryKey(courseId, userId);
        final List<Content> contents = List.of(createContentWithUserData(contentId, progressData));

        final RewardScores expectedRewardScores = new RewardScores(
                new RewardScore(100, 0, null),
                new RewardScore(100, 0, null),
                new RewardScore(0, 0, null),
                new RewardScore(0, 0, null),
                new RewardScore(0, 0, null));

        when(allRewardScoresRepository.findById(primaryKey)).thenReturn(Optional.ofNullable(allRewardScoresEntity));
        when(allRewardScoresRepository.save(any())).thenReturn(allRewardScoresEntity);
        when(contentServiceClient.queryContentsOfCourse(userId, courseId)).thenReturn(contents);
        when(rewardScoreMapper.entityToDto(allRewardScoresEntity)).thenReturn(expectedRewardScores);

        final RewardScores rewardScores = rewardService.recalculateScores(courseId, userId);

        assertThat(rewardScores, is(expectedRewardScores));
        verify(allRewardScoresRepository).findById(primaryKey);
        verify(allRewardScoresRepository).save(any());
        verify(contentServiceClient).queryContentsOfCourse(userId, courseId);
        verify(rewardScoreMapper).entityToDto(allRewardScoresEntity);

    }

    /**
     * Given courseId
     * When getScoreboard is called
     * Then the scoreboard for the course is returned
     */
    @Test
    void testGetScoreboardSuccessfully() {
        // arrange test data
        final UUID courseId = UUID.randomUUID();

        final UUID userId1 = UUID.randomUUID();
        final UUID userId2 = UUID.randomUUID();
        final UUID userId3 = UUID.randomUUID();

        final AllRewardScoresEntity rewardScores1 = dummyAllRewardScoresBuilder(courseId, userId1).power(initializeRewardScoreEntity(10)).build();
        final AllRewardScoresEntity rewardScores2 = dummyAllRewardScoresBuilder(courseId, userId2).power(initializeRewardScoreEntity(30)).build();
        final AllRewardScoresEntity rewardScores3 = dummyAllRewardScoresBuilder(courseId, userId3).build();

        final List<AllRewardScoresEntity> rewardScoresEntities = new ArrayList<>();
        rewardScoresEntities.add(rewardScores1);
        rewardScoresEntities.add(rewardScores2);
        rewardScoresEntities.add(rewardScores3);

        // mock repository
        when(allRewardScoresRepository.findAllRewardScoresEntitiesById_CourseId(courseId)).thenReturn(rewardScoresEntities);

        // act
        final List<ScoreboardItem> scoreboardItemList = rewardService.getScoreboard(courseId);

        //assert
        assertThat(scoreboardItemList.size(), is(3));
        assertThat(scoreboardItemList.get(0).getUserId(), is(rewardScores2.getId().getUserId()));
        assertThat(scoreboardItemList.get(1).getUserId(), is(rewardScores1.getId().getUserId()));
        assertThat(scoreboardItemList.get(2).getUserId(), is(rewardScores3.getId().getUserId()));

        // verify that the repository was called
        verify(allRewardScoresRepository, times(1)).findAllRewardScoresEntitiesById_CourseId(courseId);
    }

    @Test
    void testDataDeletion() throws IncompleteEventMessageException {
        // arrange test data
        final UUID courseId = UUID.randomUUID();

        final UUID userId1 = UUID.randomUUID();
        final UUID userId2 = UUID.randomUUID();
        final UUID userId3 = UUID.randomUUID();

        final AllRewardScoresEntity rewardScores1 = dummyAllRewardScoresBuilder(courseId, userId1)
                .power(initializeRewardScoreEntity(10))
                .build();
        final AllRewardScoresEntity rewardScores2 = dummyAllRewardScoresBuilder(courseId, userId2)
                .power(initializeRewardScoreEntity(30))
                .build();
        final AllRewardScoresEntity rewardScores3 = dummyAllRewardScoresBuilder(courseId, userId3).build();

        final List<AllRewardScoresEntity> rewardScoresEntities = new ArrayList<>();
        rewardScoresEntities.add(rewardScores1);
        rewardScoresEntities.add(rewardScores2);
        rewardScoresEntities.add(rewardScores3);

        final CourseChangeEvent event = CourseChangeEvent.builder()
                .courseId(courseId)
                .operation(CrudOperation.DELETE)
                .build();

        // mock repository
        when(allRewardScoresRepository.findAllRewardScoresEntitiesById_CourseId(courseId)).thenReturn(rewardScoresEntities);

        // act
        rewardService.removeRewardData(event);

        // verify that the repository was called
        verify(allRewardScoresRepository, times(1)).findAllRewardScoresEntitiesById_CourseId(courseId);
    }

    private static AllRewardScoresEntity.AllRewardScoresEntityBuilder dummyAllRewardScoresBuilder(final UUID courseId, final UUID userId) {
        return AllRewardScoresEntity.builder()
                .id(new AllRewardScoresEntity.PrimaryKey(courseId, userId))
                .health(initializeRewardScoreEntity(100))
                .strength(initializeRewardScoreEntity(0))
                .fitness(initializeRewardScoreEntity(100))
                .growth(initializeRewardScoreEntity(0))
                .power(initializeRewardScoreEntity(0));

    }

    @Test
    void testInvalidCourseEventInput(){
        final CourseChangeEvent noCourseIdEvent = CourseChangeEvent.builder().operation(CrudOperation.DELETE).build();
        final CourseChangeEvent noOperationEvent = CourseChangeEvent.builder().courseId(UUID.randomUUID()).build();

        // act
        assertThrows(IncompleteEventMessageException.class, () -> rewardService.removeRewardData(noCourseIdEvent));
        assertThrows(IncompleteEventMessageException.class, () -> rewardService.removeRewardData(noOperationEvent));

        // verify that the repository was called
        verify(allRewardScoresRepository, never()).findAllRewardScoresEntitiesById_CourseId(any());

    }

    @Test
    void testWrongOperationTypeCourseEventInput() throws IncompleteEventMessageException {
        final CourseChangeEvent createEvent = CourseChangeEvent.builder()
                .courseId(UUID.randomUUID())
                .operation(CrudOperation.CREATE)
                .build();
        final CourseChangeEvent updateEvent = CourseChangeEvent.builder()
                .courseId(UUID.randomUUID())
                .operation(CrudOperation.UPDATE)
                .build();

        rewardService.removeRewardData(createEvent);
        rewardService.removeRewardData(updateEvent);

        // verify that the repository was called
        verify(allRewardScoresRepository, never()).findAllRewardScoresEntitiesById_CourseId(any());
    }

    private static RewardScoreEntity initializeRewardScoreEntity(final int initialValue) {
        final RewardScoreEntity rewardScoreEntity = new RewardScoreEntity();
        rewardScoreEntity.setValue(initialValue);
        rewardScoreEntity.setLog(new ArrayList<>());
        return rewardScoreEntity;
    }

    private Content createContentWithUserData(final UUID contentId, final UserProgressData userProgressData) {
        return FlashcardSetAssessment.builder()
                .setId(contentId)
                .setAssessmentMetadata(AssessmentMetadata.builder().build())
                .setUserProgressData(userProgressData)
                .build();
    }

}

