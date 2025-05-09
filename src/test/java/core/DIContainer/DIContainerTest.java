package core.DIContainer;

import io.github.renatompf.ember.EmberApplication;
import io.github.renatompf.ember.annotations.content.Consumes;
import io.github.renatompf.ember.annotations.content.Produces;
import io.github.renatompf.ember.annotations.controller.Controller;
import io.github.renatompf.ember.annotations.exceptions.GlobalHandler;
import io.github.renatompf.ember.annotations.exceptions.Handles;
import io.github.renatompf.ember.annotations.http.*;
import io.github.renatompf.ember.annotations.middleware.WithMiddleware;
import io.github.renatompf.ember.annotations.parameters.PathParameter;
import io.github.renatompf.ember.annotations.parameters.QueryParameter;
import io.github.renatompf.ember.annotations.parameters.RequestBody;
import io.github.renatompf.ember.annotations.parameters.Validated;
import io.github.renatompf.ember.annotations.service.Service;
import io.github.renatompf.ember.core.*;
import io.github.renatompf.ember.enums.HttpStatusCode;
import io.github.renatompf.ember.enums.MediaType;
import io.github.renatompf.ember.enums.RequestHeader;
import io.github.renatompf.ember.exceptions.HttpException;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Length;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
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

    @BeforeEach
    void setUp() {
        container = new DIContainer("core.DIContainer");

        HeadersManager headersManager = mock(HeadersManager.class);
        lenient().when(headersManager.header("Content-Type")).thenReturn(MediaType.APPLICATION_JSON.getType());
        lenient().when(headersManager.header("Accept")).thenReturn(MediaType.APPLICATION_JSON.getType());

        // Configure Context mock
        lenient().when(context.headers()).thenReturn(headersManager);
        lenient().when(context.response()).thenReturn(responseHandler);

        middlewareCallCount.set(0);

    }

    @Test
    void mapControllerRoutes_WithNoControllers_ShouldReturnEarly() {
        // Given
        DIContainer container = new DIContainer();

        // When
        container.mapControllerRoutes(app);

        // Then
        verify(app, never()).get(anyString(), any());
        verify(app, never()).post(anyString(), any());
        verify(app, never()).put(anyString(), any());
        verify(app, never()).delete(anyString(), any());
        verify(app, never()).patch(anyString(), any());
        verify(app, never()).options(anyString(), any());
        verify(app, never()).head(anyString(), any());
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

    @Test
    void fullLifecycle_WithFailingMiddlewareHttpException_ShouldHandleError() {
        // When
        container.register(TestMiddlewareApiController.class);
        container.resolveAll();
        container.mapControllerRoutes(app);

        // Capture and execute handler
        ArgumentCaptor<Consumer<Context>> handlerCaptor = ArgumentCaptor.forClass(Consumer.class);
        verify(app).get(eq("/api/middleware/http-exception"), handlerCaptor.capture());

        handlerCaptor.getValue().accept(context);

        // Then
        ArgumentCaptor<Response<?>> responseCaptor = ArgumentCaptor.forClass(Response.class);
        verify(responseHandler).handleResponse(responseCaptor.capture());

        Response<?> capturedResponse = responseCaptor.getValue();
        assertEquals(HttpStatusCode.INTERNAL_SERVER_ERROR, capturedResponse.getStatusCode());
        assertTrue(capturedResponse.getBody() instanceof ErrorResponse);

        ErrorResponse error = (ErrorResponse) capturedResponse.getBody();
        assertEquals(HttpStatusCode.INTERNAL_SERVER_ERROR, capturedResponse.getStatusCode());
        assertEquals("Error: Middleware failure", error.getMessage());
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
        assertTrue(capturedResponse.getBody() instanceof ErrorResponse);

        ErrorResponse error = (ErrorResponse) capturedResponse.getBody();
        assertEquals(HttpStatusCode.INTERNAL_SERVER_ERROR, capturedResponse.getStatusCode());
        assertEquals("Middleware failure", error.getMessage());
        assertEquals(HttpStatusCode.INTERNAL_SERVER_ERROR, error.getStatus());
        assertNotNull(error.getTimestamp());
        assertTrue(error.getException().contains("RuntimeException"));
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
        assertEquals(capturedResponse.getContentType(), MediaType.APPLICATION_JSON.getType());
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

        assertTrue(capturedResponse.getBody() instanceof ErrorResponse);
        ErrorResponse error = (ErrorResponse) capturedResponse.getBody();
        assertEquals(HttpStatusCode.INTERNAL_SERVER_ERROR, error.getStatus());
        assertEquals("Missing query parameter: id", error.getMessage());
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
    void resolveParameter_WithRequestBody() {
        // Register and set up
        container.register(TestApiController.class);
        container.resolveAll();
        container.mapControllerRoutes(app);

        // Capture handler
        ArgumentCaptor<Consumer<Context>> handlerCaptor = ArgumentCaptor.forClass(Consumer.class);
        verify(app).post(eq("/api/body"), handlerCaptor.capture());

        // Setup body
        TestDto dto = new TestDto();
        dto.value = "test-body";
        BodyManager bodyManager = mock(BodyManager.class);
        when(context.body()).thenReturn(bodyManager);
        when(bodyManager.parseBodyAs(TestDto.class)).thenReturn(dto);

        // Execute handler
        handlerCaptor.getValue().accept(context);

        // Verify response
        ArgumentCaptor<Response<?>> responseCaptor = ArgumentCaptor.forClass(Response.class);
        verify(responseHandler).handleResponse(responseCaptor.capture());
        assertEquals("Got: test-body", responseCaptor.getValue().getBody());
    }

    @Test
    void resolveParameter_WithPathParameter() {
        // Register and set up
        container.register(TestApiController.class);
        container.resolveAll();
        container.mapControllerRoutes(app);

        // Capture handler
        ArgumentCaptor<Consumer<Context>> handlerCaptor = ArgumentCaptor.forClass(Consumer.class);
        verify(app).get(eq("/api/test/:param"), handlerCaptor.capture());

        // Setup path parameters
        Map<String, String> pathParams = Map.of("param", "123");
        when(context.pathParams()).thenReturn(new PathParameterManager(pathParams));

        // Execute handler
        handlerCaptor.getValue().accept(context);

        // Verify response
        ArgumentCaptor<Response<?>> responseCaptor = ArgumentCaptor.forClass(Response.class);
        verify(responseHandler).handleResponse(responseCaptor.capture());
        assertEquals("Got: 123", responseCaptor.getValue().getBody());
    }

    @Test
    void resolveParameter_WithMissingPathParameter() {
        // Register and set up
        container.register(TestApiController.class);
        container.resolveAll();
        container.mapControllerRoutes(app);

        // Capture handler
        ArgumentCaptor<Consumer<Context>> handlerCaptor = ArgumentCaptor.forClass(Consumer.class);
        verify(app).get(eq("/api/:id"), handlerCaptor.capture());

        // Setup path parameters with a mismatch between URL parameter and annotation
        Map<String, String> pathParams = Map.of("id", "123");  // "differentId" is not in the map
        when(context.pathParams()).thenReturn(new PathParameterManager(pathParams));

        // Execute handler
        handlerCaptor.getValue().accept(context);

        // Verify response
        ArgumentCaptor<Response<?>> responseCaptor = ArgumentCaptor.forClass(Response.class);
        verify(responseHandler).handleResponse(responseCaptor.capture());
        assertEquals("Test null", responseCaptor.getValue().getBody());
    }

    @Test
    void registerControllers_ShouldRegisterAllControllerClassesInClasspath(){
        // Given
        container.registerServices();
        container.registerControllers();
        container.resolveAll();

        assertTrue(container.isRegistered(TestApiController.class));
    }

    @Test
    void register_WithNonServiceClass_ShouldThrowException() {

        assertThrows(IllegalArgumentException.class, () -> {
            container.register(NonServiceClass.class);
        });
    }

    @Test
    void resolve_WithMultipleConstructors_ShouldUseAppropriateConstructor() {

        container.register(TestDependency.class);
        container.register(MultiConstructorService.class);
        container.resolveAll();

        MultiConstructorService service = container.resolve(MultiConstructorService.class);
        assertNotNull(service);
        assertNotNull(service.dependency);
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

    @Test
    void resolve_WithNonRegisteredService_ShouldThrowException() {
        assertThrows(IllegalStateException.class, () -> {
            container.resolve(TestDependency.class);
        });
    }

    @Test
    void resolve_WithMultiLevelDependencyChain_ShouldResolveCorrectly() {
        container.register(TestDependency.class);
        container.register(Level1Service.class);
        container.register(Level2Service.class);
        container.register(Level3Service.class);
        container.resolveAll();

        Level1Service service = container.resolve(Level1Service.class);
        assertNotNull(service);
        assertNotNull(service.service);
        assertNotNull(service.service.service);
        assertNotNull(service.service.service.dependency);
    }

    @Test
    void registerServices_ShouldRegisterAllServiceClassesInClasspath(){
        // Given
        DIContainer container = new DIContainer("core.DIContainer");
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
    void resolve_WithDifferentRegistrationOrder_ShouldWorkCorrectly() {
        // Register services in reverse dependency order
        container.register(TestServiceB.class);
        container.register(TestServiceA.class);
        container.resolveAll();

        TestServiceA serviceA = container.resolve(TestServiceA.class);
        assertNotNull(serviceA);
        assertNotNull(serviceA.serviceB);
    }

    @Test
    void registerControllers_ShouldNotRegisterNonControllerClasses(){
        // Given
        container.resolveAll();

        assertFalse(container.isRegistered(TestDependency.class));
    }

    @Test
    void resolve_ShouldCreateInstanceWithDefaultConstructor() {
        // Given
        DIContainer container = new DIContainer();
        container.register(TestSimpleService.class);

        // When
        TestSimpleService service = container.resolve(TestSimpleService.class);

        // Then
        assertNotNull(service);
    }

    @Test
    void mapControllerRoutes_ShouldHandleAllPathCombinations() {

        // Register all controllers
        container.register(EmptyBaseController.class);
        container.register(TrailingSlashController.class);
        container.register(NoTrailingSlashController.class);
        container.resolveAll();
        container.mapControllerRoutes(app);

        // Verify all path combinations
        verify(app).head(eq("/test"), any());  // Empty base + /test
        verify(app).post(eq("/test"), any()); // Empty base + test
        verify(app).put(eq("/api/users"), any());   // /api/ + users
        verify(app).options(eq("/api/users"), any());   // /api/ + /users
        verify(app).delete(eq("/api/users"), any()); // /api + /users
        verify(app).patch(eq("/api/users"), any());  // /api + users
    }

    @Test
    void mapControllerRoutes_ShouldHandleAllResponseTypes() {
        // Register and resolve the controller
        container.register(TestResponseController.class);
        container.resolveAll();
        container.mapControllerRoutes(app);

        // Capture handlers for each route
        ArgumentCaptor<Consumer<Context>> getHandlerCaptor = ArgumentCaptor.forClass(Consumer.class);
        ArgumentCaptor<Consumer<Context>> postHandlerCaptor = ArgumentCaptor.forClass(Consumer.class);
        ArgumentCaptor<Consumer<Context>> putHandlerCaptor = ArgumentCaptor.forClass(Consumer.class);

        verify(app).get(eq("/test-api-response/response"), getHandlerCaptor.capture());
        verify(app).post(eq("/test-api-response/null"), postHandlerCaptor.capture());
        verify(app).put(eq("/test-api-response/object"), putHandlerCaptor.capture());

        // Test case 1: Result is already a Response object
        getHandlerCaptor.getValue().accept(context);
        ArgumentCaptor<Response<?>> responseCaptor1 = ArgumentCaptor.forClass(Response.class);
        verify(responseHandler).handleResponse(responseCaptor1.capture());
        assertEquals("direct response", responseCaptor1.getValue().getBody());
        assertEquals(HttpStatusCode.OK, responseCaptor1.getValue().getStatusCode());

        // Test case 2: Result is null
        postHandlerCaptor.getValue().accept(context);
        ArgumentCaptor<Response<?>> responseCaptor2 = ArgumentCaptor.forClass(Response.class);
        verify(responseHandler, times(2)).handleResponse(responseCaptor2.capture());
        assertNull(responseCaptor2.getValue().getBody());
        assertEquals(HttpStatusCode.OK, responseCaptor2.getValue().getStatusCode());

        // Test case 3: Result is a regular object
        putHandlerCaptor.getValue().accept(context);
        ArgumentCaptor<Response<?>> responseCaptor3 = ArgumentCaptor.forClass(Response.class);
        verify(responseHandler, times(3)).handleResponse(responseCaptor3.capture());
        assertEquals("plain string", responseCaptor3.getValue().getBody());
        assertEquals(HttpStatusCode.OK, responseCaptor3.getValue().getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON.getType(), responseCaptor3.getValue().getContentType());
    }

    public static class FailingMiddlewareHttpException implements Middleware {
        @Override
        public void handle(Context context) throws Exception {
            throw new HttpException(HttpStatusCode.INTERNAL_SERVER_ERROR, "Error: Middleware failure");
        }
    }

    public static class TestDto {
        private String value;

        public String getValue() {
            return value;
        }
    }

    @Controller("/api")
    public static class TestApiController {
        @Get("/test")
        public Response<String> handleGet(@QueryParameter("id") String id) {
            return Response.ok().body("Test " + id).build();
        }

        @Get("/test/:param")
        public Response<String> handleGetPathParameter(@PathParameter("param") String param) {
            return Response.ok().body("Got: " + param).build();
        }

        @Get("/:id")
        public Response<String> handleGetDifferentPathParameter(@PathParameter("differentId") String id) {
            return Response.ok().body("Test " + (id == null ? "null" : id)).build();
        }


        @Post("/body")
        public Response<String> handlePost(@RequestBody TestDto body) {
            return Response.ok().body("Got: " + body.getValue()).build();
        }

    }

    @Controller("/api/middleware")
    @WithMiddleware(TestMiddleware.class)
    public static class TestMiddlewareApiController {
        @Get("/test")
        @WithMiddleware(TestMiddleware.class)
        public Response<String> handleGet(@QueryParameter("id") String id) {
            return Response.ok().body("Test " + id).build();
        }

        @Get("/fail")
        @WithMiddleware(FailingMiddleware.class)
        public Response<String> handleFail() {
            return Response.ok().body("This should not be reached").build();
        }

        @Get("/http-exception")
        @WithMiddleware(FailingMiddlewareHttpException.class)
        public Response<String> handleFailHttpException() {
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
        public Response<String> handleGet(@QueryParameter("id") String id) {
            return Response.ok().body("Test " + id + " with dependency: " + testDependency.getData()).build();
        }
    }

    public static class NonServiceClass {
    }

    @Service
    public static class MultiConstructorService {
        private final TestDependency dependency;

        public MultiConstructorService() {
            this.dependency = null;
        }

        public MultiConstructorService(TestDependency dependency) {
            this.dependency = dependency;
        }
    }

    @Service
    public static class Level1Service {
        private final Level2Service service;

        public Level1Service(Level2Service service) {
            this.service = service;
        }
    }

    @Service
    public static class Level2Service {
        private final Level3Service service;

        public Level2Service(Level3Service service) {
            this.service = service;
        }
    }

    @Service
    public static class Level3Service {
        private final TestDependency dependency;

        public Level3Service(TestDependency dependency) {
            this.dependency = dependency;
        }
    }

    @Service
    public static class TestSimpleService {
        private String nonFinalField;

        TestSimpleService() {
        }
    }

    @Controller
    public static class EmptyBaseController {
        @Head("/test")
        public void testSlash() {
        }

        @Post("test")
        public void testNoSlash() {
        }
    }

    @Controller("/api/")
    public static class TrailingSlashController {
        @Options("/users")
        public void testWithSlash() {
        }

        @Put("users")
        public void testWithoutSlash() {
        }
    }

    @Controller("/api")
    public static class NoTrailingSlashController {
        @Delete("/users")
        public void testWithSlash() {
        }

        @Patch("users")
        public void testWithoutSlash() {
        }
    }

    @Controller("/test-api-response")
    public static class TestResponseController {
        @Get("/response")
        public Response<String> returnsResponse() {
            return Response.ok().body("direct response").build();
        }

        @Post("/null")
        public Object returnsNull() {
            return null;
        }

        @Put("/object")
        public String returnsObject() {
            return "plain string";
        }
    }

    @Test
    void handleException_WithRegisteredHandler_ShouldUseCustomHandler() {
        // Given
        container.register(TestExceptionHandler.class);
        container.register(TestExceptionController.class);
        container.resolveAll();  // Resolve instances first
        container.registerExceptionHandlers();  // Now check for handler instances
        container.mapControllerRoutes(app);

        // Capture and execute handler
        ArgumentCaptor<Consumer<Context>> handlerCaptor = ArgumentCaptor.forClass(Consumer.class);
        verify(app).get(eq("/api/exception/custom"), handlerCaptor.capture());

        handlerCaptor.getValue().accept(context);

        // Then
        ArgumentCaptor<Response<?>> responseCaptor = ArgumentCaptor.forClass(Response.class);
        verify(responseHandler).handleResponse(responseCaptor.capture());

        Response<?> response = responseCaptor.getValue();

        assertTrue(response.getBody() instanceof ErrorResponse);
        ErrorResponse errorResponse = (ErrorResponse) response.getBody();
        assertEquals(HttpStatusCode.BAD_REQUEST, response.getStatusCode());
        assertTrue(errorResponse.getMessage().contains("Custom error"));
        assertEquals("/api/exception/custom", errorResponse.getPath());
        assertEquals(CustomTestException.class.getName(), errorResponse.getException());
    }

    @Test
    void handleException_WithHttpException_ShouldHandleCorrectly() {
        // Given
        container.register(TestExceptionHandler.class);
        container.register(TestExceptionController.class);
        container.resolveAll();
        container.registerExceptionHandlers();
        container.mapControllerRoutes(app);

        // Capture and execute handler
        ArgumentCaptor<Consumer<Context>> handlerCaptor = ArgumentCaptor.forClass(Consumer.class);
        verify(app).get(eq("/api/exception/http"), handlerCaptor.capture());

        handlerCaptor.getValue().accept(context);

        // Then
        ArgumentCaptor<Response<?>> responseCaptor = ArgumentCaptor.forClass(Response.class);
        verify(responseHandler).handleResponse(responseCaptor.capture());

        Response<?> response = responseCaptor.getValue();

        assertTrue(response.getBody() instanceof ErrorResponse);
        ErrorResponse errorResponse = (ErrorResponse) response.getBody();
        assertEquals(HttpStatusCode.FORBIDDEN, response.getStatusCode());
        assertEquals("Access denied", errorResponse.getMessage());
        assertEquals("/api/exception/http", errorResponse.getPath());
        assertEquals(HttpException.class.getName(), errorResponse.getException());
    }

    @Test
    void handleException_WithUnhandledException_ShouldUseDefaultHandling() {
        // When
        container.register(TestExceptionController.class);
        container.resolveAll();
        container.registerExceptionHandlers();
        container.mapControllerRoutes(app);

        // Capture and execute handler
        ArgumentCaptor<Consumer<Context>> handlerCaptor = ArgumentCaptor.forClass(Consumer.class);
        verify(app).get(eq("/api/exception/unhandled"), handlerCaptor.capture());

        handlerCaptor.getValue().accept(context);

        // Then
        ArgumentCaptor<Response<?>> responseCaptor = ArgumentCaptor.forClass(Response.class);
        verify(responseHandler).handleResponse(responseCaptor.capture());

        Response<?> response = responseCaptor.getValue();

        assertTrue(response.getBody() instanceof ErrorResponse);
        ErrorResponse errorResponse = (ErrorResponse) response.getBody();

        assertEquals(HttpStatusCode.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Unhandled error", errorResponse.getMessage());
        assertNull(errorResponse.getPath());
        assertEquals(RuntimeException.class.getName(), errorResponse.getException());
    }

    @Test
    void registerGlobalHandlers_ShouldRegisterHandlerClasses() {
        // Given
        DIContainer container = new DIContainer("core.DIContainer");

        // When
        container.registerGlobalHandlers();

        // Then
        assertTrue(container.isRegistered(TestExceptionHandler.class));
    }

    @Test
    void registerGlobalHandlers_WithInvalidPath_ShouldNotFindAnyHandlers() throws Exception {
        // Given
        DIContainer container = new DIContainer("invalid.package.path");

        // When
        container.registerGlobalHandlers();

        // Then
        Method findHandlers = DIContainer.class.getDeclaredMethod("findHandlers");
        findHandlers.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Class<?>> result = (List<Class<?>>) findHandlers.invoke(container);
        assertTrue(result.isEmpty());
    }

    @Test
    void findHandlers_ShouldFindAnnotatedClasses() throws Exception {
        // Given
        DIContainer container = new DIContainer("core.DIContainer");

        // When
        Method findHandlers = DIContainer.class.getDeclaredMethod("findHandlers");
        findHandlers.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Class<?>> result = (List<Class<?>>) findHandlers.invoke(container);

        // Then
        assertFalse(result.isEmpty());
        assertTrue(result.contains(TestExceptionHandler.class));
        assertTrue(result.stream()
                .allMatch(clazz -> clazz.isAnnotationPresent(GlobalHandler.class)));
    }

    @Test
    void findHandlers_WithNoHandlers_ShouldReturnEmptyList() throws Exception {
        // Given
        DIContainer container = new DIContainer("core.utils");

        // When
        Method findHandlers = DIContainer.class.getDeclaredMethod("findHandlers");
        findHandlers.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Class<?>> result = (List<Class<?>>) findHandlers.invoke(container);

        // Then
        assertTrue(result.isEmpty());
    }

    private static class CustomTestException extends Exception {
        public CustomTestException(String message) {
            super(message);
        }
    }

    @GlobalHandler
    private static class TestExceptionHandler {
        @Handles(CustomTestException.class)
        public Response<ErrorResponse> handleCustomException(CustomTestException ex) {
            return Response.status(HttpStatusCode.BAD_REQUEST)
                    .body(new ErrorResponse(
                            HttpStatusCode.BAD_REQUEST,
                            ex.getMessage(),
                            "/api/exception/custom",
                            ex.getClass().getName()))
                    .build();
        }

        @Handles(HttpException.class)
        public Response<ErrorResponse> handleHttpException(HttpException ex) {
            return Response.status(ex.getStatus())
                    .body(new ErrorResponse(
                            ex.getStatus(),
                            ex.getMessage(),
                            "/api/exception/http",
                            ex.getClass().getName()))
                    .build();
        }
    }

    public static class ValidateDto {
        @NotBlank
        @NotNull
        private String value;

        public String getValue() {
            return value;
        }
    }

    @Controller("/api/exception")
    public static class TestExceptionController {
        @Get("/custom")
        public Response<String> throwCustomException() throws CustomTestException {
            System.out.println("Throwing custom exception");
            throw new CustomTestException("Custom error");
        }

        @Get("/http")
        public Response<String> throwHttpException() {
            throw new HttpException(HttpStatusCode.FORBIDDEN, "Access denied");
        }

        @Get("/unhandled")
        public Response<String> throwUnhandledException() {
            throw new RuntimeException("Unhandled error");
        }
    }

    @Controller("/api/content")
    public static class TestNegotiationContentController {
        @Post
        @Consumes(MediaType.APPLICATION_JSON)
        public void consumeJson() {}

        @Get
        @Produces(MediaType.APPLICATION_JSON)
        public void produceJson() {}

        @Get("/consume-and-produce")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public void consumeAndProduce() {}

        @Post("/test-validation")
        public Response<String> testValidation(@Validated @RequestBody ValidateDto dto) {
            return Response.ok().body(dto.getValue()).build();
        }

        @Post("/test-no-validation")
        public Response<String> testNoValidation(@RequestBody ValidateDto dto) {
            return Response.ok().body("Not Validate").build();
        }

        @Get("/test-validation/:id")
        public Response<String> testValidationWithPath(@NotNull @NotBlank @Length(min = 5, max = 10) @PathParameter("id") String id) {
            return Response.ok().body(id).build();
        }

        @Get("/test-validation")
        public Response<String> testValidationWithQuery(@NotNull @NotBlank @Length(min = 5, max = 10) @QueryParameter("id") String id) {
            return Response.ok().body(id).build();
        }

    }



    @Test
    void validateContentType_WithValidContentType_ShouldPass() throws NoSuchMethodException {
        Method method = TestNegotiationContentController.class.getDeclaredMethod("consumeJson");
        Context context = mock(Context.class);
        HeadersManager headers = mock(HeadersManager.class);

        when(context.headers()).thenReturn(headers);
        when(headers.header("Content-Type")).thenReturn(MediaType.APPLICATION_JSON.getType());

        ContentNegotiationManager manager = new ContentNegotiationManager();
        assertDoesNotThrow(() -> manager.validateContentType(context, method));
    }

    @Test
    void validateContentType_WithInvalidContentType_ShouldThrowException() throws NoSuchMethodException {
        Method method = TestNegotiationContentController.class.getDeclaredMethod("consumeJson");
        Context context = mock(Context.class);
        HeadersManager headers = mock(HeadersManager.class);

        when(context.headers()).thenReturn(headers);
        when(headers.header("Content-Type")).thenReturn(MediaType.APPLICATION_XML.getType());

        ContentNegotiationManager manager = new ContentNegotiationManager();
        assertThrows(HttpException.class, () -> manager.validateContentType(context, method));
    }

    @Test
    void negotiateResponseType_WithValidAcceptHeader_ShouldReturnMediaType() throws NoSuchMethodException {
        Method method = TestNegotiationContentController.class.getDeclaredMethod("produceJson");
        Context context = mock(Context.class);
        HeadersManager headers = mock(HeadersManager.class);

        when(context.headers()).thenReturn(headers);
        when(headers.header("Accept")).thenReturn(MediaType.APPLICATION_JSON.getType());

        ContentNegotiationManager manager = new ContentNegotiationManager();
        MediaType result = manager.negotiateResponseType(context, method);

        assertEquals(MediaType.APPLICATION_JSON, result);
    }

    @Test
    void negotiateResponseType_WithUnsupportedAcceptHeader_ShouldThrowException() throws NoSuchMethodException {

        Method method = TestNegotiationContentController.class.getDeclaredMethod("produceJson");
        Context context = mock(Context.class);
        HeadersManager headers = mock(HeadersManager.class);

        when(context.headers()).thenReturn(headers);
        when(headers.header("Accept")).thenReturn(MediaType.APPLICATION_XML.getType());

        ContentNegotiationManager manager = new ContentNegotiationManager();
        assertThrows(HttpException.class, () -> manager.negotiateResponseType(context, method));
    }

    @Test
    void handleWithMiddleware_ShouldValidateAndNegotiateContentType() throws Exception {
        // Register and resolve the controller
        container.register(TestNegotiationContentController.class);
        container.resolveAll();
        container.mapControllerRoutes(app);

        // Verify that the route is mapped
        verify(app).post(eq("/api/content/"), any());

        // Capture the route handler
        ArgumentCaptor<Consumer<Context>> handlerCaptor = ArgumentCaptor.forClass(Consumer.class);
        verify(app).post(eq("/api/content/"), handlerCaptor.capture());

        // Mock headers for content negotiation
        HeadersManager headersManager = mock(HeadersManager.class);
        lenient().when(context.headers()).thenReturn(headersManager);
        lenient().when(headersManager.header("Content-Type")).thenReturn(MediaType.APPLICATION_JSON.getType());
        lenient().when(headersManager.header("Accept")).thenReturn(MediaType.APPLICATION_JSON.getType());

        // Execute the handler
        handlerCaptor.getValue().accept(context);

        // Verify that the response has the correct content type
        verify(headersManager).setHeader(RequestHeader.CONTENT_TYPE.getHeaderName(), MediaType.APPLICATION_JSON.getType());
    }

    @Test
    void testValidation_WithValidDto_ShouldReturnOkResponse() {
        // Given
        container.register(TestNegotiationContentController.class);
        container.resolveAll();
        container.mapControllerRoutes(app);

        // Capture handler
        ArgumentCaptor<Consumer<Context>> handlerCaptor = ArgumentCaptor.forClass(Consumer.class);
        verify(app).post(eq("/api/content/test-validation"), handlerCaptor.capture());

        // Setup valid DTO
        ValidateDto validDto = new ValidateDto();
        validDto.value = "Valid Value";
        BodyManager bodyManager = mock(BodyManager.class);
        when(context.body()).thenReturn(bodyManager);
        when(bodyManager.parseBodyAs(ValidateDto.class)).thenReturn(validDto);

        // Execute handler
        handlerCaptor.getValue().accept(context);

        // Verify response
        ArgumentCaptor<Response<?>> responseCaptor = ArgumentCaptor.forClass(Response.class);
        verify(responseHandler).handleResponse(responseCaptor.capture());
        assertEquals("Valid Value", responseCaptor.getValue().getBody());
        assertEquals(HttpStatusCode.OK, responseCaptor.getValue().getStatusCode());
    }

    @Test
    void testValidation_WithInvalidDto_ShouldReturnBadRequest() {
        // Given
        container.register(TestNegotiationContentController.class);
        container.resolveAll();
        container.mapControllerRoutes(app);

        // Capture handler
        ArgumentCaptor<Consumer<Context>> handlerCaptor = ArgumentCaptor.forClass(Consumer.class);
        verify(app).post(eq("/api/content/test-validation"), handlerCaptor.capture());

        // Setup invalid DTO
        ValidateDto invalidDto = new ValidateDto();
        BodyManager bodyManager = mock(BodyManager.class);
        when(context.body()).thenReturn(bodyManager);
        when(bodyManager.parseBodyAs(ValidateDto.class)).thenReturn(invalidDto);

        // Execute handler
        handlerCaptor.getValue().accept(context);

        // Verify response
        ArgumentCaptor<Response<?>> responseCaptor = ArgumentCaptor.forClass(Response.class);
        verify(responseHandler).handleResponse(responseCaptor.capture());

        Response<?> capturedResponse = responseCaptor.getValue();
        assertEquals(HttpStatusCode.BAD_REQUEST, capturedResponse.getStatusCode());
        assertInstanceOf(ErrorResponse.class, capturedResponse.getBody());

        ErrorResponse error = (ErrorResponse) capturedResponse.getBody();
        assertEquals(HttpStatusCode.BAD_REQUEST, error.getStatus());
        assertNotNull(error.getMessage());
    }

    @Test
    void testNoValidation_WithInvalidDto_ShouldReturnOkResponse() {
        // Given
        container.register(TestNegotiationContentController.class);
        container.resolveAll();
        container.mapControllerRoutes(app);

        // Capture handler
        ArgumentCaptor<Consumer<Context>> handlerCaptor = ArgumentCaptor.forClass(Consumer.class);
        verify(app).post(eq("/api/content/test-no-validation"), handlerCaptor.capture());

        // Setup invalid DTO
        ValidateDto invalidDto = new ValidateDto(); // Missing required value
        BodyManager bodyManager = mock(BodyManager.class);
        when(context.body()).thenReturn(bodyManager);
        when(bodyManager.parseBodyAs(ValidateDto.class)).thenReturn(invalidDto);

        // Execute handler
        handlerCaptor.getValue().accept(context);

        // Verify response
        ArgumentCaptor<Response<?>> responseCaptor = ArgumentCaptor.forClass(Response.class);
        verify(responseHandler).handleResponse(responseCaptor.capture());
        assertEquals("Not Validate", responseCaptor.getValue().getBody());
        assertEquals(HttpStatusCode.OK, responseCaptor.getValue().getStatusCode());
    }

    @Test
    void testValidationWithPath_ShouldValidateAndReturnId() {
        // Given
        container.register(TestNegotiationContentController.class);
        container.resolveAll();
        container.mapControllerRoutes(app);

        // Capture the route handler
        ArgumentCaptor<Consumer<Context>> handlerCaptor = ArgumentCaptor.forClass(Consumer.class);
        verify(app).get(eq("/api/content/test-validation/:id"), handlerCaptor.capture());

        // Setup path parameters
        Map<String, String> pathParams = Map.of("id", "validId");
        when(context.pathParams()).thenReturn(new PathParameterManager(pathParams));

        // When
        handlerCaptor.getValue().accept(context);

        // Then
        ArgumentCaptor<Response<?>> responseCaptor = ArgumentCaptor.forClass(Response.class);
        verify(responseHandler).handleResponse(responseCaptor.capture());

        Response<?> capturedResponse = responseCaptor.getValue();
        assertEquals(HttpStatusCode.OK, capturedResponse.getStatusCode());
        assertEquals("validId", capturedResponse.getBody());
    }

    @Test
    void testValidationWithPath_ShouldValidateInvalidInputAndReturnBadRequest() {
        // Given
        container.register(TestNegotiationContentController.class);
        container.resolveAll();
        container.mapControllerRoutes(app);

        // Capture the route handler
        ArgumentCaptor<Consumer<Context>> handlerCaptor = ArgumentCaptor.forClass(Consumer.class);
        verify(app).get(eq("/api/content/test-validation/:id"), handlerCaptor.capture());

        // Setup path parameters
        Map<String, String> pathParams = Map.of("id", "id");
        when(context.pathParams()).thenReturn(new PathParameterManager(pathParams));

        // When
        handlerCaptor.getValue().accept(context);

        // Then
        ArgumentCaptor<Response<?>> responseCaptor = ArgumentCaptor.forClass(Response.class);
        verify(responseHandler).handleResponse(responseCaptor.capture());

        Response<?> capturedResponse = responseCaptor.getValue();
        assertInstanceOf(ErrorResponse.class, capturedResponse.getBody());

        ErrorResponse error = (ErrorResponse) capturedResponse.getBody();
        assertEquals(HttpStatusCode.BAD_REQUEST, error.getStatus());
        assertNotNull(error.getMessage());
    }

    @Test
    void testValidationWithQueryParameter_ShouldValidateAndReturnId() {
        // Given
        container.register(TestNegotiationContentController.class);
        container.resolveAll();
        container.mapControllerRoutes(app);

        // Capture the route handler
        ArgumentCaptor<Consumer<Context>> handlerCaptor = ArgumentCaptor.forClass(Consumer.class);
        verify(app).get(eq("/api/content/test-validation"), handlerCaptor.capture());

        // Setup path parameters
        String queryParams = "id=validId";
        when(context.queryParams()).thenReturn(new QueryParameterManager(queryParams));

        // When
        handlerCaptor.getValue().accept(context);

        // Then
        ArgumentCaptor<Response<?>> responseCaptor = ArgumentCaptor.forClass(Response.class);
        verify(responseHandler).handleResponse(responseCaptor.capture());

        Response<?> capturedResponse = responseCaptor.getValue();
        assertEquals(HttpStatusCode.OK, capturedResponse.getStatusCode());
        assertEquals("validId", capturedResponse.getBody());
    }

    @Test
    void testValidationWithQueryParameter_ShouldValidateInvalidInputAndReturnBadRequest() {
        // Given
        container.register(TestNegotiationContentController.class);
        container.resolveAll();
        container.mapControllerRoutes(app);

        // Capture the route handler
        ArgumentCaptor<Consumer<Context>> handlerCaptor = ArgumentCaptor.forClass(Consumer.class);
        verify(app).get(eq("/api/content/test-validation"), handlerCaptor.capture());

        // Setup path parameters
        String queryParams = "id=ididididididididididididid";
        when(context.queryParams()).thenReturn(new QueryParameterManager(queryParams));

        // When
        handlerCaptor.getValue().accept(context);

        // Then
        ArgumentCaptor<Response<?>> responseCaptor = ArgumentCaptor.forClass(Response.class);
        verify(responseHandler).handleResponse(responseCaptor.capture());

        Response<?> capturedResponse = responseCaptor.getValue();
        assertInstanceOf(ErrorResponse.class, capturedResponse.getBody());

        ErrorResponse error = (ErrorResponse) capturedResponse.getBody();
        assertEquals(HttpStatusCode.BAD_REQUEST, error.getStatus());
        assertNotNull(error.getMessage());
    }

}