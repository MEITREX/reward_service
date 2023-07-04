package de.unistuttgart.iste.gits.reward.service;

import de.unistuttgart.iste.gits.generated.dto.Content;
import de.unistuttgart.iste.gits.generated.dto.ContentMetadata;
import de.unistuttgart.iste.gits.generated.dto.MediaContent;
import de.unistuttgart.iste.gits.generated.dto.UserProgressData;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.graphql.client.GraphQlClient;
import org.springframework.graphql.client.HttpGraphQlClient;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Client for the content service, allowing to query contents with user progress data.
 * <p>
 * TODO: The calls to the content service are blocking. We should consider figuring out reactive calls.
 * TODO: The error handling is minimal.
 */
@Component
public class ContentServiceClient {

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

        // we must use media content here because the content type is an interface
        // that cannot be used for deserialization
        List<ContentWithUserProgressData[]> result = graphQlClient.document(query)
                .variable("userId", userId)
                .variable("chapterIds", chapterIds)
                .retrieve("contentsByChapterIds")
                .toEntityList(ContentWithUserProgressData[].class)
                .block();

        if (result == null) {
            return List.of();
        }
        return result.stream()
                .flatMap(Arrays::stream)
                .map(ContentWithUserProgressData::toContent)
                .toList();
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

}
