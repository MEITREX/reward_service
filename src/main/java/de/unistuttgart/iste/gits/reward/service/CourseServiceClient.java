package de.unistuttgart.iste.gits.reward.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.graphql.client.GraphQlClient;
import org.springframework.graphql.client.HttpGraphQlClient;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Client for the course service.
 * <p>
 * Can retrieve all chapter ids for a course and the course id for a content id.
 * <p>
 * TODO: The calls to the course service are blocking. We should consider figuring out reactive calls.
 * TODO: The error handling is minimal.
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
                    coursesById(ids: [$courseId]) {
                        chapters {
                            elements {
                                id
                            }
                        }
                    },
                }
                """;

        return graphQlClient.document(query)
                .variable("courseId", courseId)
                .retrieve("coursesById[0].chapters.elements")
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

        return graphQlClient.document(query)
                .variable("contentId", contentId)
                .retrieve("resourceById[0].availableCourses[0]")
                .toEntity(UUID.class)
                .doOnError(e -> log.error("Error while retrieving course id from course service", e))
                .retry(RETRY_COUNT)
                .doOnNext(courseId -> contentIdToCourseIdCache.put(contentId, courseId))
                .block();
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
}
