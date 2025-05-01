package core;

import io.github.renatompf.ember.EmberApplication;
import io.github.renatompf.ember.annotations.controller.Controller;
import io.github.renatompf.ember.annotations.http.Get;
import io.github.renatompf.ember.annotations.middleware.WithMiddleware;
import io.github.renatompf.ember.annotations.parameters.QueryParameter;
import io.github.renatompf.ember.annotations.service.Service;
import io.github.renatompf.ember.core.*;
import io.github.renatompf.ember.enums.HttpStatusCode;
import io.github.renatompf.ember.enums.MediaType;
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
        ArgumentCaptor<Response<?>> responseCaptor = ArgumentCaptor.forClass(Response.class);
        verify(responseHandler).handleResponse(responseCaptor.capture());

        Response<?> capturedResponse = responseCaptor.getValue();

        assertEquals(HttpStatusCode.OK, capturedResponse.getStatusCode());
        assertEquals("Test 123", capturedResponse.getBody());
        assertTrue(capturedResponse.getContentType().equals(MediaType.APPLICATION_JSON.getType()));
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
        ArgumentCaptor<Response<?>> responseCaptor = ArgumentCaptor.forClass(Response.class);
        verify(responseHandler).handleResponse(responseCaptor.capture());

        Response<?> capturedResponse = responseCaptor.getValue();
        assertEquals(HttpStatusCode.INTERNAL_SERVER_ERROR, capturedResponse.getStatusCode());
        assertEquals("Error: Middleware failure", capturedResponse.getBody());

    }

    @Service
    public static class TestDependency {
        public String getData() {
            return "test data";
        }
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
        ArgumentCaptor<Response<?>> responseCaptor = ArgumentCaptor.forClass(Response.class);
        verify(responseHandler).handleResponse(responseCaptor.capture());

        Response<?> capturedResponse = responseCaptor.getValue();
        assertEquals(HttpStatusCode.INTERNAL_SERVER_ERROR, capturedResponse.getStatusCode());
        assertTrue(capturedResponse.getBody().toString().contains("Failed to invoke controller method: handleGet"));
    }

    // ========= End of Test Classes =========

    @BeforeEach
    void setUp() {
        container = new DIContainer();
        lenient().when(context.response()).thenReturn(responseHandler);
        middlewareCallCount.set(0);

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
        ArgumentCaptor<Response<?>> responseCaptor = ArgumentCaptor.forClass(Response.class);
        verify(responseHandler).handleResponse(responseCaptor.capture());

        Response<?> capturedResponse = responseCaptor.getValue();
        assertEquals(HttpStatusCode.OK, capturedResponse.getStatusCode());
        assertEquals("Test 123", capturedResponse.getBody());
        assertTrue(capturedResponse.getContentType().equals(MediaType.APPLICATION_JSON.getType()));
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
        ArgumentCaptor<Response<?>> responseCaptor = ArgumentCaptor.forClass(Response.class);
        verify(responseHandler).handleResponse(responseCaptor.capture());

        Response<?> capturedResponse = responseCaptor.getValue();
        assertEquals(HttpStatusCode.OK, capturedResponse.getStatusCode());
        assertEquals("Test 123 with dependency: test data", capturedResponse.getBody());
        assertTrue(capturedResponse.getContentType().equals(MediaType.APPLICATION_JSON.getType()));
        ;
    }

    @Controller("/api")
    public static class TestApiController {
        @Get("/test")
        public Response<String> handleGet(@QueryParameter("id") String id) {
            return Response.ok().body("Test " + id).build();
        }
    }

    @Service
    public static class TestServiceA {
        private final TestServiceB serviceB;
        private String message = "A";

        public TestServiceA(TestServiceB serviceB) {
            this.serviceB = serviceB;
        }

        public String getFromB() {
            return serviceB.getMessage();
        }

        public String getMessage() {
            return message;
        }

    }

    @Service
    public static class TestServiceB {
        private final TestServiceA serviceA;
        private String message = "B";

        public TestServiceB(TestServiceA serviceA) {
            this.serviceA = serviceA;
        }

        public String getMessage() {
            return message;
        }

    }

    @Service
    public static class TestServiceC {
        private final TestServiceA serviceA;
        private String message = "C";

        public TestServiceC(TestServiceA serviceA) {
            this.serviceA = serviceA;
        }

        public String getMessage() {
            return message;
        }
    }

    @Service
    public static class TestServiceD {
        private String message = "D";

        public String getMessage() {
            return message;
        }
    }

    @Service
    public static class TestServiceE {
        private final TestServiceD serviceD;
        private String message = "E";

        public TestServiceE(TestServiceD serviceD) {
            this.serviceD = serviceD;
        }

        public String getMessage() {
            return message;
        }
    }

    @Test
    void shouldResolveCircularDependencyWithConstructorInjection() {
        // Register both services
        container.register(TestServiceA.class);
        container.register(TestServiceB.class);

        // Resolve the services
        container.resolveAll();

        // Get instance of ServiceA
        TestServiceA serviceA = container.resolve(TestServiceA.class);

        // Verify that the circular dependency is resolved
        assertNotNull(serviceA);
        assertNotNull(serviceA.serviceB);
        assertEquals("B", serviceA.getFromB());
    }

    @Test
    void shouldResolveComplexCircularDependencyChain() {
        // Register all services
        container.register(TestServiceA.class);
        container.register(TestServiceB.class);
        container.register(TestServiceC.class);

        // Resolve the services
        container.resolveAll();

        // Get instances and verify they're properly wired
        TestServiceA serviceA = container.resolve(TestServiceA.class);
        TestServiceB serviceB = container.resolve(TestServiceB.class);
        TestServiceC serviceC = container.resolve(TestServiceC.class);

        // Verify that all services are created
        assertNotNull(serviceA);
        assertNotNull(serviceB);
        assertNotNull(serviceC);

        // Verify the circular chain is properly connected
        assertSame(serviceB, serviceA.serviceB);
        assertSame(serviceA, serviceB.serviceA);
        assertSame(serviceA, serviceC.serviceA);

        // Verify messages to ensure proper initialization
        assertEquals("A", serviceA.getMessage());
        assertEquals("B", serviceB.getMessage());
        assertEquals("C", serviceC.getMessage());
    }

    @Test
    void shouldResolveComplexCircularDependencyChainAndClassWithNoConstructor() {
        // Register all services
        container.register(TestServiceD.class);
        container.register(TestServiceE.class);

        // Resolve the services
        container.resolveAll();

        // Get instances and verify they're properly wired
        TestServiceD serviceD = container.resolve(TestServiceD.class);
        TestServiceE serviceE = container.resolve(TestServiceE.class);

        // Verify that all services are created
        assertNotNull(serviceD);
        assertNotNull(serviceE);

        // Verify the circular chain is properly connected
        assertSame(serviceD, serviceE.serviceD);

        // Verify messages to ensure proper initialization
        assertEquals("D", serviceD.getMessage());
        assertEquals("D", serviceE.serviceD.getMessage());
    }

    @Controller("/api/middleware")
    @WithMiddleware(TestMiddleware.class)
    public static class TestMiddlewareApiController {
        @Get("/test")
        @WithMiddleware(TestMiddleware.class)
        public Response handleGet(@QueryParameter("id") String id) {
            return Response.ok().body("Test " + id).build();
        }

        @Get("/fail")
        @WithMiddleware(FailingMiddleware.class)
        public Response handleFail() {
            return Response.ok().body("This should not be reached").build();
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
            return Response.ok().body("Test " + id + " with dependency: " + testDependency.getData()).build();
        }
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

