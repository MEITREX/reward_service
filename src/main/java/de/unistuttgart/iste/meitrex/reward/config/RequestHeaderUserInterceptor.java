package de.unistuttgart.iste.meitrex.reward.config;

import de.unistuttgart.iste.meitrex.common.user_handling.RequestHeaderUserProcessor;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.server.*;
import reactor.core.publisher.Mono;

/**
 * This class is used to add data from the request headers to the GraphQL context.
 */
@Configuration
public class RequestHeaderUserInterceptor implements WebGraphQlInterceptor {
    @NotNull
    @Override
    @SneakyThrows
    public Mono<WebGraphQlResponse> intercept(@NotNull WebGraphQlRequest request, @NotNull Chain chain) {
        RequestHeaderUserProcessor.process(request);
        return chain.next(request);
    }
}
