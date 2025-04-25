package core;

import io.ember.core.Context;
import io.ember.core.MiddlewareChain;
import io.ember.core.RouteMatchResult;
import io.ember.core.Router;
import io.ember.enums.HttpMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class RouterTest {
    
    private Router router;

    @BeforeEach
    void setUp() {
        router = new Router();
    }

    @Test
    void register_ShouldAddRouteWithHandler() {
        // Given
        HttpMethod method = HttpMethod.GET;
        String path = "/test";
        Consumer<Context> handler = context -> {};

        // When
        router.register(method, path, handler);

        // Then
        RouteMatchResult result = router.getRoute(method, path);
        assertNotNull(result);
        assertEquals(Map.of(), result.parameters());
    }

    @Test
    void register_ShouldAddRouteWithMiddlewareChain() {
        // Given
        HttpMethod method = HttpMethod.POST;
        String path = "/test";
        MiddlewareChain chain = new MiddlewareChain(List.of(), context -> {});

        // When
        router.register(method, path, chain);

        // Then
        RouteMatchResult result = router.getRoute(method, path);
        assertNotNull(result);
        assertSame(chain, result.middlewareChain());
    }

    @Test
    void getRoute_ShouldReturnNull_WhenNoMatchingRoute() {
        // Given
        router.register(HttpMethod.GET, "/test", context -> {});

        // When
        RouteMatchResult result = router.getRoute(HttpMethod.POST, "/test");

        // Then
        assertNull(result);
    }

    @Test
    void getRoute_ShouldMatchExactPathsFirst() {
        // Given
        router.register(HttpMethod.GET, "/users/:id", context -> {});
        router.register(HttpMethod.GET, "/users/profile", context -> {});

        // When
        RouteMatchResult result = router.getRoute(HttpMethod.GET, "/users/profile");

        // Then
        assertNotNull(result);
        assertTrue(result.parameters().isEmpty());
    }

    @Test
    void getRoute_ShouldExtractPathParameters() {
        // Given
        router.register(HttpMethod.GET, "/users/:id", context -> {});

        // When
        RouteMatchResult result = router.getRoute(HttpMethod.GET, "/users/123");

        // Then
        assertNotNull(result);
        assertEquals(Map.of("id", "123"), result.parameters());
    }

    @Test
    void getRoute_ShouldMatchOptionalParameters() {
        // Given
        router.register(HttpMethod.GET, "/users/:id?", _ -> {});

        // When
        RouteMatchResult withParam = router.getRoute(HttpMethod.GET, "/users/123");
        RouteMatchResult withoutParam = router.getRoute(HttpMethod.GET, "/users/");

        // Then
        assertNotNull(withParam);
        assertEquals(Map.of("id", "123"), withParam.parameters());
        
        assertNotNull(withoutParam);

        Map<String, Object> expected = new HashMap<>();
        expected.put("id", null);
        assertEquals(expected, withoutParam.parameters());
    }

    @Test
    void getRoute_ShouldMatchWildcardPaths() {
        // Given
        router.register(HttpMethod.GET, "/files/*path", context -> {});

        // When
        RouteMatchResult result = router.getRoute(HttpMethod.GET, "/files/docs/report.pdf");

        // Then
        assertNotNull(result);
        assertEquals(Map.of("*", "docs/report.pdf"), result.parameters());
    }

    @Test
    void getRoute_ShouldPrioritizeLongerExactPaths() {
        // Given
        router.register(HttpMethod.GET, "/api/v1", context -> {});
        router.register(HttpMethod.GET, "/api/v1/users", context -> {});

        // When
        RouteMatchResult result = router.getRoute(HttpMethod.GET, "/api/v1/users");

        // Then
        assertNotNull(result);
        assertTrue(result.parameters().isEmpty());
    }

    @Test
    void getRoute_ShouldHandleMultipleParameters() {
        // Given
        router.register(HttpMethod.GET, "/users/:userId/posts/:postId", context -> {});

        // When
        RouteMatchResult result = router.getRoute(HttpMethod.GET, "/users/123/posts/456");

        // Then
        assertNotNull(result);
        assertEquals(Map.of(
            "userId", "123",
            "postId", "456"
        ), result.parameters());
    }
}
