package core;

import io.ember.EmberApplication;
import io.ember.annotations.controller.Controller;
import io.ember.annotations.http.Get;
import io.ember.annotations.middleware.WithMiddleware;
import io.ember.annotations.parameters.QueryParameter;
import io.ember.annotations.service.Service;
import io.ember.core.*;
import io.ember.exceptions.CircularDependencyException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DIContainerTest {
    private DIContainer container;

    @Mock
    private EmberApplication app;

    @Mock
    private Context context;

    @Mock
    private ResponseHandler responseHandler;

    // ========= Test Classes =========

    // Static variable to track middleware call count
    public static AtomicInteger middlewareCallCount = new AtomicInteger(0);

    @Controller("/api")
    public static class TestApiController {
        @Get("/test")
        public Response handleGet(@QueryParameter("id") String id) {
            return Response.ok("Test " + id);
        }
    }

    public static class FailingMiddleware implements Middleware {
        @Override
        public void handle(Context context) throws Exception {
            throw new RuntimeException("Middleware failure");
        }
    }

    public static class TestMiddleware implements Middleware {
        @Override
        public void handle(Context context) {
            middlewareCallCount.incrementAndGet();
        }
    }

    @Controller("/api/middleware")
    @WithMiddleware(TestMiddleware.class)
    public static class TestMiddlewareApiController {
        @Get("/test")
        @WithMiddleware(TestMiddleware.class)
        public Response handleGet(@QueryParameter("id") String id) {
            return Response.ok("Test " + id);
        }

        @Get("/fail")
        @WithMiddleware(FailingMiddleware.class)
        public Response handleFail() {
            return Response.ok("This should not be reached");
        }
    }

    @Service
    public static class TestDependency {
        public String getData() {
            return "test data";
        }
    }

    @Controller("/api/service")
    public static class TestServiceController {
        private final TestDependency testDependency;

        public TestServiceController(TestDependency testDependency) {
            this.testDependency = testDependency;
        }

        @Get("/test")
        public Response handleGet(@QueryParameter("id") String id) {
            return Response.ok("Test " + id + " with dependency: " + testDependency.getData());
        }
    }

    // ========= End of Test Classes =========

    @BeforeEach
    void setUp() {
        container = new DIContainer();
        lenient().when(context.response()).thenReturn(responseHandler);
        middlewareCallCount.set(0);

    }

    @Test
    void fullLifecycle_WithValidController_ShouldMapAndHandleRequests() {
        // When
        container.register(TestApiController.class);
        container.resolveAll();
        container.mapControllerRoutes(app);

        // Capture the route handler
        ArgumentCaptor<Consumer<Context>> handlerCaptor = ArgumentCaptor.forClass(Consumer.class);
        verify(app).get(eq("/api/test"), handlerCaptor.capture());

        // Then simulate request handling
        when(context.queryParams()).thenReturn(new QueryParameterManager("id=123"));

        // Execute the captured handler
        handlerCaptor.getValue().accept(context);

        // Verify response
        verify(responseHandler).sendJson("Test 123", 200);
    }

    @Test
    void fullLifecycle_WithMiddleware_ShouldExecuteMiddlewareAndController() {
        // When
        container.register(TestMiddlewareApiController.class);
        container.resolveAll();
        container.mapControllerRoutes(app);

        // Capture and execute handler
        ArgumentCaptor<Consumer<Context>> handlerCaptor = ArgumentCaptor.forClass(Consumer.class);
        verify(app).get(eq("/api/middleware/test"), handlerCaptor.capture());
        when(context.queryParams()).thenReturn(new QueryParameterManager("id=123"));

        handlerCaptor.getValue().accept(context);

        // Then
        assertEquals(2, middlewareCallCount.get()); // Both controller and method middleware executed
    }

    @Test
    void fullLifecycle_WithFailingMiddleware_ShouldHandleError() {
        // When
        container.register(TestMiddlewareApiController.class);
        container.resolveAll();
        container.mapControllerRoutes(app);

        // Capture and execute handler
        ArgumentCaptor<Consumer<Context>> handlerCaptor = ArgumentCaptor.forClass(Consumer.class);
        verify(app).get(eq("/api/middleware/fail"), handlerCaptor.capture());

        handlerCaptor.getValue().accept(context);

        // Then
        verify(responseHandler).internalServerError("Error: Middleware failure");
        verify(responseHandler, never()).sendJson(any(), anyInt());
    }

    @Test
    void fullLifecycle_WithMissingQueryParameter_ShouldHandleError() {

        // When
        container.register(TestApiController.class);
        container.resolveAll();
        container.mapControllerRoutes(app);

        // Capture and execute handler
        ArgumentCaptor<Consumer<Context>> handlerCaptor = ArgumentCaptor.forClass(Consumer.class);
        verify(app).get(eq("/api/test"), handlerCaptor.capture());
        when(context.queryParams()).thenReturn(new QueryParameterManager("")); // Empty query parameters
        handlerCaptor.getValue().accept(context);

        // Then
        verify(responseHandler).internalServerError(contains("Failed to invoke controller method: handleGet"));
    }

    @Test
    void fullLifecycle_WithCircularDependency_ShouldThrowException() {

        @Controller("/api/circular")
         class CircularDependencyController {
            private final CircularDependencyController dependency;

            public CircularDependencyController(CircularDependencyController dependency) {
                this.dependency = dependency;
            }

            @Get("/test")
            public Response handleGet() {
                return Response.ok("This should not be reached");
            }
        }

        // Given
        container.register(CircularDependencyController.class);

        // When & Then
        CircularDependencyException exception = assertThrows(CircularDependencyException.class, () -> container.resolveAll());
        assertEquals("Circular dependency detected for: " + CircularDependencyController.class.getName(), exception.getMessage());
    }

    @Test
    void fullLifecycle_WithServiceDependency_ShouldInjectService() {
        // Given
        container.register(TestDependency.class);
        container.register(TestApiController.class);
        container.resolveAll();
        container.mapControllerRoutes(app);

        // Capture the route handler
        ArgumentCaptor<Consumer<Context>> handlerCaptor = ArgumentCaptor.forClass(Consumer.class);
        verify(app).get(eq("/api/test"), handlerCaptor.capture());

        // Then simulate request handling
        when(context.queryParams()).thenReturn(new QueryParameterManager("id=123"));

        // Execute the captured handler
        handlerCaptor.getValue().accept(context);

        // Verify response
        verify(responseHandler).sendJson("Test 123", 200);
    }

    @Test
    void fullLifecycle_WithServiceDependency_ShouldInjectServiceIntoController() {
        // Given
        container.register(TestDependency.class);
        container.register(TestServiceController.class);
        container.resolveAll();
        container.mapControllerRoutes(app);

        // Capture the route handler
        ArgumentCaptor<Consumer<Context>> handlerCaptor = ArgumentCaptor.forClass(Consumer.class);
        verify(app).get(eq("/api/service/test"), handlerCaptor.capture());

        // Then simulate request handling
        when(context.queryParams()).thenReturn(new QueryParameterManager("id=123"));

        // Execute the captured handler
        handlerCaptor.getValue().accept(context);

        // Verify response
        verify(responseHandler).sendJson(eq("Test 123 with dependency: test data"), eq(200));
    }

    @Test
    void registerServices_ShouldRegisterAllServiceClassesInClasspath(){
        // Given
        container.registerServices();
        container.resolveAll();

        assertTrue(container.isRegistered(TestDependency.class));
    }

    @Test
    void registerServices_ShouldNotRegisterNonServiceClasses(){
        // Given
        container.resolveAll();

        assertFalse(container.isRegistered(TestApiController.class));
    }

    @Test
    void registerControllers_ShouldRegisterAllControllerClassesInClasspath(){
        // Given
        container.registerControllers();
        container.resolveAll();

        assertTrue(container.isRegistered(TestApiController.class));
    }

    @Test
    void registerControllers_ShouldNotRegisterNonControllerClasses(){
        // Given
        container.resolveAll();

        assertFalse(container.isRegistered(TestDependency.class));
    }

}

