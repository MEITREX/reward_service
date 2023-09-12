package de.unistuttgart.iste.gits.reward.service;

import de.unistuttgart.iste.gits.common.event.*;
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
     * Given a courseId and userID
     * when initializeRewardScores is called
     * Then create a new allRewardScoresEntity with the default values
     */
    @Test
    void testInitializeRewardScoreEntity() {
        UUID courseId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        AllRewardScoresEntity expectedEntity = dummyAllRewardScoresBuilder(courseId, userId).build();

        when(allRewardScoresRepository.save(any())).thenReturn(expectedEntity);

        AllRewardScoresEntity rewardScoresEntity = rewardService.initializeRewardScores(courseId, userId);

        verify(allRewardScoresRepository).save(any());
        assertThat(rewardScoresEntity, is(expectedEntity));

    }

    /**
     * Given a progressEvent
     * when calculateScoresOnContentWorkedOn is called
     * Then update the rewardScores and return it
     */
    @Test
    void testCalculateScoresOnContentWorkedOn() {
        UUID contentId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        UUID userID = UUID.randomUUID();
        UUID chapterId1 = UUID.randomUUID();
        UUID chapterId2 = UUID.randomUUID();

        AllRewardScoresEntity.PrimaryKey primaryKey = new AllRewardScoresEntity.PrimaryKey(courseId, userID);

        UserProgressData progressData = UserProgressData.builder().build();
        List<UUID> chapterIds = List.of(chapterId1, chapterId2);
        List<Content> contents = List.of(createContentWithUserData(contentId, progressData));

        UserProgressLogEvent event = UserProgressLogEvent.builder()
                .userId(userID)
                .contentId(contentId)
                .correctness(1)
                .hintsUsed(0)
                .success(true)
                .build();

        AllRewardScoresEntity allRewardScoresEntity = dummyAllRewardScoresBuilder(courseId, userID).build();

        RewardScores expectedRewardScores = new RewardScores(
                new RewardScore(100, 0, null),
                new RewardScore(100, 0, null),
                new RewardScore(0, 0, null),
                new RewardScore(0, 0, null),
                new RewardScore(0, 0, null));

        when(allRewardScoresRepository.findById(primaryKey)).thenReturn(Optional.ofNullable(allRewardScoresEntity));
        when(allRewardScoresRepository.save(any())).thenReturn(allRewardScoresEntity);
        when(courseServiceClient.getCourseIdForContent(event.getContentId())).thenReturn(courseId);
        when(courseServiceClient.getChapterIds(courseId)).thenReturn(chapterIds);
        when(contentServiceClient.getContentsWithUserProgressData(userID, chapterIds)).thenReturn(contents);
        when(rewardScoreMapper.entityToDto(allRewardScoresEntity)).thenReturn(expectedRewardScores);

        RewardScores rewardScores = rewardService.calculateScoresOnContentWorkedOn(event);

        assertThat(rewardScores, is(expectedRewardScores));
        verify(allRewardScoresRepository).save(any());
        verify(allRewardScoresRepository).findById(primaryKey);
        verify(courseServiceClient).getCourseIdForContent(contentId);
        verify(courseServiceClient).getChapterIds(courseId);
        verify(contentServiceClient).getContentsWithUserProgressData(userID, chapterIds);
        verify(rewardScoreMapper).entityToDto(allRewardScoresEntity);

    }

    /**
     * Given a rewardScore
     * when recalculateScores is called
     * Then update the rewardScores and return it
     */
    @Test
    void testRecalculateScores() {
        UUID courseId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID contentId = UUID.randomUUID();
        UUID chapterId1 = UUID.randomUUID();
        UUID chapterId2 = UUID.randomUUID();

        UserProgressData progressData = UserProgressData.builder().build();
        AllRewardScoresEntity allRewardScoresEntity = dummyAllRewardScoresBuilder(courseId, userId).build();
        AllRewardScoresEntity.PrimaryKey primaryKey = new AllRewardScoresEntity.PrimaryKey(courseId, userId);
        List<UUID> chapterIds = List.of(chapterId1, chapterId2);
        List<Content> contents = List.of(createContentWithUserData(contentId, progressData));

        RewardScores expectedRewardScores = new RewardScores(
                new RewardScore(100, 0, null),
                new RewardScore(100, 0, null),
                new RewardScore(0, 0, null),
                new RewardScore(0, 0, null),
                new RewardScore(0, 0, null));

        when(allRewardScoresRepository.findById(primaryKey)).thenReturn(Optional.ofNullable(allRewardScoresEntity));
        when(allRewardScoresRepository.save(any())).thenReturn(allRewardScoresEntity);
        when(courseServiceClient.getChapterIds(courseId)).thenReturn(chapterIds);
        when(contentServiceClient.getContentsWithUserProgressData(userId, chapterIds)).thenReturn(contents);
        when(rewardScoreMapper.entityToDto(allRewardScoresEntity)).thenReturn(expectedRewardScores);

        RewardScores rewardScores = rewardService.recalculateScores(courseId, userId);

        assertThat(rewardScores, is(expectedRewardScores));
        verify(allRewardScoresRepository).findById(primaryKey);
        verify(allRewardScoresRepository).save(any());
        verify(courseServiceClient).getChapterIds(courseId);
        verify(contentServiceClient).getContentsWithUserProgressData(userId, chapterIds);
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

    @Test
    void testDataDeletion() {
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

        CourseChangeEvent event = CourseChangeEvent.builder().courseId(courseId).operation(CrudOperation.DELETE).build();

        // mock repository
        when(allRewardScoresRepository.findAllRewardScoresEntitiesById_CourseId(courseId)).thenReturn(rewardScoresEntities);

        // act
        rewardService.removeRewardData(event);


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

    @Test
    void testInvalidCourseEventInput(){
        CourseChangeEvent noCourseIdEvent = CourseChangeEvent.builder().operation(CrudOperation.DELETE).build();
        CourseChangeEvent noOperationEvent = CourseChangeEvent.builder().courseId(UUID.randomUUID()).build();


        // act
        assertThrows(NullPointerException.class, () -> rewardService.removeRewardData(noCourseIdEvent));
        assertThrows(NullPointerException.class, () -> rewardService.removeRewardData(noOperationEvent));

        // verify that the repository was called
        verify(allRewardScoresRepository, never()).findAllRewardScoresEntitiesById_CourseId(any());

    }

    @Test
    void testWrongOperationTypeCourseEventInput(){
        CourseChangeEvent createEvent = CourseChangeEvent.builder().courseId(UUID.randomUUID()).operation(CrudOperation.CREATE).build();
        CourseChangeEvent updateEvent = CourseChangeEvent.builder().courseId(UUID.randomUUID()).operation(CrudOperation.UPDATE).build();

        rewardService.removeRewardData(createEvent);
        rewardService.removeRewardData(updateEvent);

        // verify that the repository was called
        verify(allRewardScoresRepository, never()).findAllRewardScoresEntitiesById_CourseId(any());
    }

    private static RewardScoreEntity initializeRewardScoreEntity(int initialValue) {
        RewardScoreEntity rewardScoreEntity = new RewardScoreEntity();
        rewardScoreEntity.setValue(initialValue);
        rewardScoreEntity.setLog(new ArrayList<>());
        return rewardScoreEntity;
    }

    private Content createContentWithUserData(UUID contentId, UserProgressData userProgressData) {
        return FlashcardSetAssessment.builder()
                .setId(contentId)
                .setAssessmentMetadata(AssessmentMetadata.builder().build())
                .setUserProgressData(userProgressData)
                .build();
    }

}

