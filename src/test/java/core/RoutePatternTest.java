package core;

import io.ember.core.RoutePattern;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RoutePatternTest {

    @Test
    void shouldMatchExactPath() {
        RoutePattern pattern = new RoutePattern("/users/profile");
        assertTrue(pattern.matches("/users/profile"));
        assertFalse(pattern.matches("/users/123"));
        assertTrue(pattern.extractParameters("/users/profile").isEmpty());
    }

    @Test
    void shouldMatchPathWithParameters() {
        RoutePattern pattern = new RoutePattern("/users/:id");
        assertTrue(pattern.matches("/users/123"));
        assertFalse(pattern.matches("/users/"));
        assertEquals(Map.of("id", "123"), pattern.extractParameters("/users/123"));
    }

    @Test
    void shouldMatchPathWithOptionalParameters() {
        RoutePattern pattern = new RoutePattern("/users/:id?");
        assertTrue(pattern.matches("/users/123"));
        assertTrue(pattern.matches("/users/"));
        assertEquals(Map.of("id", "123"), pattern.extractParameters("/users/123"));
        Map<String, Object> expected = new HashMap<>();
        expected.put("id", null);
        assertEquals(expected, pattern.extractParameters("/users/"));
    }

    @Test
    void shouldMatchPathWithMultipleParameters() {
        RoutePattern pattern = new RoutePattern("/users/:userId/posts/:postId");
        assertTrue(pattern.matches("/users/123/posts/456"));
        assertFalse(pattern.matches("/users/123/posts/"));
        assertEquals(
            Map.of("userId", "123", "postId", "456"),
            pattern.extractParameters("/users/123/posts/456")
        );
    }

    @Test
    void shouldMatchWildcardPath() {
        RoutePattern pattern = new RoutePattern("/files/*path");
        assertTrue(pattern.matches("/files/docs/report.pdf"));
        assertTrue(pattern.matches("/files/images/avatar.png"));
        assertEquals(
            Map.of("*", "docs/report.pdf"),
            pattern.extractParameters("/files/docs/report.pdf")
        );
    }

    @Test
    void shouldNotMatchInvalidPaths() {
        RoutePattern pattern = new RoutePattern("/users/:id");
        assertFalse(pattern.matches("/posts/123"));
        assertFalse(pattern.matches("/users/123/extra"));
        assertTrue(pattern.extractParameters("/posts/123").isEmpty());
    }

    @Test
    void shouldPreserveOriginalPath() {
        String path = "/users/:id/posts/*rest";
        RoutePattern pattern = new RoutePattern(path);
        assertEquals(path, pattern.getRawPath());
    }

    @Test
    void shouldHandleRootPath() {
        RoutePattern pattern = new RoutePattern("/");
        assertFalse(pattern.matches("/something"));
        assertFalse(pattern.matches("/"));
        assertTrue(pattern.extractParameters("/").isEmpty());
    }

    @Test
    void shouldHandleTrailingSlashes() {
        RoutePattern pattern = new RoutePattern("/users/:id/");
        assertTrue(pattern.matches("/users/123"));
        assertFalse(pattern.matches("/users/123/"));
        assertEquals(Map.of("id", "123"), pattern.extractParameters("/users/123"));
    }

    @Test
    void shouldHandleSpecialCharactersInParameters() {
        RoutePattern pattern = new RoutePattern("/users/:id");
        assertTrue(pattern.matches("/users/123-456"));
        assertTrue(pattern.matches("/users/user@domain.com"));
        assertEquals(Map.of("id", "123-456"), pattern.extractParameters("/users/123-456"));
    }
}
