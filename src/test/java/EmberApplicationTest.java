import io.github.renatompf.ember.EmberApplication;
import io.github.renatompf.ember.core.Context;
import io.github.renatompf.ember.core.Middleware;
import io.github.renatompf.ember.core.RouteGroup;
import io.github.renatompf.ember.core.RouteMatchResult;
import io.github.renatompf.ember.enums.HttpMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

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

    @Test
    void shouldRegisterRouteWithMiddleware() {
        // Given
        String path = "/test";
        Consumer<Context> handler = ctx -> {};
        List<Middleware> middleware = List.of(ctx -> {});

        // When
        app.registerRoute(HttpMethod.GET, path, handler, middleware);

        // Then
        RouteMatchResult match = app.getRouter().getRoute(HttpMethod.GET, path);
        assertNotNull(match);
    }

    @Test
    void shouldAddGlobalMiddleware() {
        // Given
        Middleware middleware = ctx -> {};

        // When
        EmberApplication result = app.use(middleware);

        // Then
        assertSame(app, result);
        assertTrue(app.getMiddleware().contains(middleware));
    }

    @Test
    void shouldSetErrorHandler() {
        // Given
        BiConsumer<Context, Exception> handler = (ctx, ex) -> {};

        // When
        EmberApplication result = app.onError(handler);

        // Then
        assertSame(app, result);
        assertSame(handler, app.getErrorHandler());
    }

    @Test
    void shouldCreateRouteGroup() {
        // Given
        String prefix = "/api";
        Consumer<RouteGroup> groupConfig = group ->
            group.get("/users", ctx -> {});

        // When
        EmberApplication result = app.group(prefix, groupConfig);

        // Then
        assertSame(app, result);
        RouteMatchResult match = app.getRouter().getRoute(HttpMethod.GET, "/api/users");
        assertNotNull(match);
    }
}
