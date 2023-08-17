package de.unistuttgart.iste.gits.reward.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.graphql.client.GraphQlClient;
import org.springframework.graphql.client.HttpGraphQlClient;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;

/**
 * Client for the course service.
 * <p>
 * Can retrieve all chapter ids for a course and the course id for a content id.
 */
@Component
@Slf4j
public class CourseServiceClient {

    @Value("${course_service.url}")
    private String courseServiceUrl;

    private static final Map<UUID, UUID> contentIdToCourseIdCache = new HashMap<>();
    private static final Map<UUID, List<UUID>> courseIdToChapterIdsCache = new HashMap<>();

    private static final int RETRY_COUNT = 3;

    /**
     * Calls the course service to get all chapter ids for a course.
     * Answers are cached to avoid unnecessary calls to the course service.
     *
     * @param courseId the course id
     * @return the list of chapter ids
     */
    public List<UUID> getChapterIds(UUID courseId) {
        if (courseIdToChapterIdsCache.containsKey(courseId)) {
            return courseIdToChapterIdsCache.get(courseId);
        }
        WebClient webClient = WebClient.builder().baseUrl(courseServiceUrl).build();

        GraphQlClient graphQlClient = HttpGraphQlClient.builder(webClient).build();

        String query = """
                query($courseId: UUID!) {
                    coursesByIds(ids: [$courseId]) {
                        chapters {
                            elements {
                                id
                            }
                        }
                    },
                }
                """;

        log.info("Sending coursesById query to course service with courseId {}", courseId);
        return graphQlClient.document(query)
                .variable("courseId", courseId)
                .retrieve("coursesByIds[0].chapters.elements")
                .toEntityList(ChapterWithId.class)
                .doOnError(e -> log.error("Error while retrieving chapter ids from course service", e))
                .retry(RETRY_COUNT)
                .map(chapters -> chapters.stream().map(ChapterWithId::id).toList())
                .doOnNext(chapterIds -> courseIdToChapterIdsCache.put(courseId, chapterIds))
                .block();
    }

    /**
     * Call the course service to get the course id for a content id.
     * Answers are cached to avoid unnecessary calls to the course service.
     *
     * @param contentId the content id
     * @return the course id
     */
    public UUID getCourseIdForContent(UUID contentId) {
        try {
            if (contentIdToCourseIdCache.containsKey(contentId)) {
                return contentIdToCourseIdCache.get(contentId);
            }
            WebClient webClient = WebClient.builder().baseUrl(courseServiceUrl).build();
            GraphQlClient graphQlClient = HttpGraphQlClient.builder(webClient).build();

            String query = """
                    query($contentId: UUID!) {
                        resourceById(ids: [$contentId]) {
                            availableCourses
                        }
                    }
                    """;

            log.info("Sending resourceById query to course service with contentId {}", contentId);
            UUID courseId = graphQlClient.document(query)
                    .variable("contentId", contentId)
                    .retrieve("resourceById[0].availableCourses[0]")
                    .toEntity(UUID.class)
                    .retry(RETRY_COUNT)
                    .block();

            if (courseId == null) {
                throw new CourseServiceConnectionException("Could not retrieve the courseId of content with id "
                                                           + contentId
                                                           + ". The course of the content might not be available for the"
                                                           + " user or the content or course was deleted.", null);
            }

            contentIdToCourseIdCache.put(contentId, courseId);
            return courseId;

        } catch (Exception e) {
            throw new CourseServiceConnectionException("Error while retrieving course id from course service", e);
        }
    }

    /**
     * Clears all caches.
     */
    public static void clearCache() {
        contentIdToCourseIdCache.clear();
        courseIdToChapterIdsCache.clear();
    }

    // helper class for deserialization
    private record ChapterWithId(UUID id) {
    }

    public static class CourseServiceConnectionException extends RuntimeException {
        public CourseServiceConnectionException(String message, Exception cause) {
            super(message, cause);
        }
    }
}
