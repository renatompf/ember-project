import io.github.renatompf.ember.EmberApplication;
import io.github.renatompf.ember.core.routing.RouteMatchResult;
import io.github.renatompf.ember.core.server.Context;
import io.github.renatompf.ember.enums.HttpMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class EmberApplicationTest {

    private EmberApplication app;

    @Mock
    private Context context;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        app = new EmberApplication();
    }

    @Test
    void shouldRegisterGetRoute() {
        // Given
        String path = "/test";
        Consumer<Context> handler = ctx -> {};

        // When
        EmberApplication result = app.get(path, handler);

        // Then
        assertSame(app, result);
        RouteMatchResult match = app.getRouter().getRoute(HttpMethod.GET, path);
        assertNotNull(match);
    }

    @Test
    void shouldRegisterPostRoute() {
        // Given
        String path = "/test";
        Consumer<Context> handler = ctx -> {};

        // When
        EmberApplication result = app.post(path, handler);

        // Then
        assertSame(app, result);
        RouteMatchResult match = app.getRouter().getRoute(HttpMethod.POST, path);
        assertNotNull(match);
    }

    @Test
    void shouldRegisterPutRoute() {
        // Given
        String path = "/test";
        Consumer<Context> handler = ctx -> {};

        // When
        EmberApplication result = app.put(path, handler);

        // Then
        assertSame(app, result);
        RouteMatchResult match = app.getRouter().getRoute(HttpMethod.PUT, path);
        assertNotNull(match);
    }

    @Test
    void shouldRegisterDeleteRoute() {
        // Given
        String path = "/test";
        Consumer<Context> handler = ctx -> {};

        // When
        EmberApplication result = app.delete(path, handler);

        // Then
        assertSame(app, result);
        RouteMatchResult match = app.getRouter().getRoute(HttpMethod.DELETE, path);
        assertNotNull(match);
    }

    @Test
    void shouldRegisterPatchRoute() {
        // Given
        String path = "/test";
        Consumer<Context> handler = ctx -> {};

        // When
        EmberApplication result = app.patch(path, handler);

        // Then
        assertSame(app, result);
        RouteMatchResult match = app.getRouter().getRoute(HttpMethod.PATCH, path);
        assertNotNull(match);
    }

    @Test
    void shouldRegisterOptionsRoute() {
        // Given
        String path = "/test";
        Consumer<Context> handler = ctx -> {};

        // When
        EmberApplication result = app.options(path, handler);

        // Then
        assertSame(app, result);
        RouteMatchResult match = app.getRouter().getRoute(HttpMethod.OPTIONS, path);
        assertNotNull(match);
    }

    @Test
    void shouldRegisterHeadRoute() {
        // Given
        String path = "/test";
        Consumer<Context> handler = ctx -> {};

        // When
        EmberApplication result = app.head(path, handler);

        // Then
        assertSame(app, result);
        RouteMatchResult match = app.getRouter().getRoute(HttpMethod.HEAD, path);
        assertNotNull(match);
    }


}
