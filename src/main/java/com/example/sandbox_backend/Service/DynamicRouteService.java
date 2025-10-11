package com.example.sandbox_backend.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionWriter;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.List;
import java.util.Map;

@Service
public class DynamicRouteService {
    private static final Logger log = LoggerFactory.getLogger(DynamicRouteService.class);

    private final RouteDefinitionWriter routeDefinitionWriter;
    private final ApplicationEventPublisher publisher;

    public DynamicRouteService(RouteDefinitionWriter routeDefinitionWriter,
                               ApplicationEventPublisher publisher) {
        this.routeDefinitionWriter = routeDefinitionWriter;
        this.publisher = publisher;
    }

    public RouteDefinition addUserRoute(String userId, String targetUri) {
        if (userId == null || userId.isEmpty()) {
            throw new IllegalArgumentException("userId cannot be null or empty");
        }

        RouteDefinition routeDefinition = new RouteDefinition();
        routeDefinition.setId("user-" + userId);

        PredicateDefinition pathPredicate = new PredicateDefinition();
        pathPredicate.setName("Path");
        pathPredicate.setArgs(Map.of("pattern", "/user/" + userId + "/**"));
        routeDefinition.setPredicates(List.of(pathPredicate));

        FilterDefinition rewriteFilter = new FilterDefinition();
        rewriteFilter.setName("RewritePath");
        rewriteFilter.setArgs(Map.of(
                "regexp", "/user/" + userId + "/(?<segment>.*)",
                "replacement", "/${segment}"
        ));
        routeDefinition.setFilters(List.of(rewriteFilter));

        routeDefinition.setUri(URI.create(targetUri));

        routeDefinitionWriter.save(Mono.just(routeDefinition)).subscribe();
        publisher.publishEvent(new RefreshRoutesEvent(this));

        log.info("Added dynamic route for user {} to target {}", userId, targetUri);
        return routeDefinition;
    }

    public void removeUserRoute(String userId) {
        String routeId = "user-" + userId;

        routeDefinitionWriter.delete(Mono.just(routeId))
                .onErrorResume(error -> {
                    if (error.getMessage() != null && error.getMessage().contains("RouteDefinition not found")) {
                        log.debug("Route {} already removed or doesn't exist", routeId);
                    } else {
                        log.error("Error removing route for user: {}", userId, error);
                    }
                    return Mono.empty();
                })
                .subscribe();

        publisher.publishEvent(new RefreshRoutesEvent(this));
        log.info("Removed route for user: {}", userId);
    }
}