package de.unistuttgart.iste.gits.reward.service;

import de.unistuttgart.iste.gits.generated.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.graphql.client.GraphQlClient;
import org.springframework.graphql.client.HttpGraphQlClient;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;

/**
 * Client for the content service, allowing to query contents with user progress data.
 */
@Component
@Slf4j
public class ContentServiceClient {

    private static final long RETRY_COUNT = 3;

    @Value("${content_service.url}")
    private String contentServiceUrl;

    /**
     * Calls the content service to get the contents for a list of chapter ids.
     *
     * @param userId     the user id
     * @param chapterIds the list of chapter ids
     * @return the list of contents
     */
    public List<Content> getContentsWithUserProgressData(UUID userId,
                                                         List<UUID> chapterIds) {
        try {
            WebClient webClient = WebClient.builder().baseUrl(contentServiceUrl).build();

            GraphQlClient graphQlClient = HttpGraphQlClient.builder(webClient).build();

            String query = """
                    query($userId: UUID!, $chapterIds: [UUID!]!) {
                        contentsByChapterIds(chapterIds: $chapterIds) {
                            id
                            metadata {
                                name
                                tagNames
                                suggestedDate
                                type
                                chapterId
                                rewardPoints
                            }
                            progressDataForUser(userId: $userId) {
                                userId
                                contentId
                                learningInterval
                                nextLearnDate
                                lastLearnDate
                                log {
                                    timestamp
                                    success
                                    correctness
                                    hintsUsed
                                    timeToComplete
                                }
                            }
                        }
                    }
                                    
                    """;

            log.info("Sending contentsByChapterIds query to course service with chapterIds {}", chapterIds);

            // we must use media content here because the content type is an interface
            // that cannot be used for deserialization
            List<ContentWithUserProgressData[]> result = graphQlClient.document(query)
                    .variable("userId", userId)
                    .variable("chapterIds", chapterIds)
                    .retrieve("contentsByChapterIds")
                    .toEntityList(ContentWithUserProgressData[].class)
                    .retry(RETRY_COUNT)
                    .block();

            if (result == null) {
                return List.of();
            }
            return result.stream()
                    .flatMap(Arrays::stream)
                    .map(ContentWithUserProgressData::toContent)
                    .toList();
        } catch (Exception e) {
            throw new ContentServiceConnectionException("Error while fetching contents from content service", e);
        }
    }


    // helper class to deserialize the result of the graphql query
    private record ContentWithUserProgressData(UUID id, ContentMetadata metadata,
                                               UserProgressData progressDataForUser) {

        private Content toContent() {
            return MediaContent.builder()
                    .setId(id)
                    .setMetadata(metadata)
                    .setUserProgressData(progressDataForUser)
                    .build();
        }
    }

    public static class ContentServiceConnectionException extends RuntimeException {
        public ContentServiceConnectionException(String message, Exception e) {
            super(message, e);
        }
    }

}
