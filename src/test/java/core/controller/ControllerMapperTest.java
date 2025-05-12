package core.controller;

import io.github.renatompf.ember.EmberApplication;
import io.github.renatompf.ember.annotations.controller.Controller;
import io.github.renatompf.ember.annotations.http.*;
import io.github.renatompf.ember.annotations.middleware.WithMiddleware;
import io.github.renatompf.ember.annotations.parameters.PathParameter;
import io.github.renatompf.ember.annotations.parameters.Validated;
import io.github.renatompf.ember.core.controller.ControllerMapper;
import io.github.renatompf.ember.core.exception.ExceptionHandlerMethod;
import io.github.renatompf.ember.core.exception.ExceptionHandlerRegistry;
import io.github.renatompf.ember.core.http.ErrorResponse;
import io.github.renatompf.ember.core.http.HeadersManager;
import io.github.renatompf.ember.core.http.Response;
import io.github.renatompf.ember.core.http.ResponseHandler;
import io.github.renatompf.ember.core.parameter.ContentNegotiationManager;
import io.github.renatompf.ember.core.parameter.ParameterResolver;
import io.github.renatompf.ember.core.server.Context;
import io.github.renatompf.ember.core.server.Middleware;
import io.github.renatompf.ember.core.validation.ValidationManager;
import io.github.renatompf.ember.enums.HttpStatusCode;
import io.github.renatompf.ember.enums.RequestHeader;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ControllerMapperTest {

    @Mock
    private ExceptionHandlerRegistry exceptionHandlerRegistry;

    @Mock
    private Validator validator;

    @Mock
    private ParameterResolver parameterResolver;

    @Mock
    private ValidationManager validationManager;

    @Mock
    private EmberApplication app;

    @Mock
    private Context context;

    @Mock
    private ResponseHandler responseHandler;

    @Mock
    private HeadersManager headersManager;

    @InjectMocks
    private ControllerMapper controllerMapper;

    @Captor
    private ArgumentCaptor<String> routeCaptor;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // Use reflection to inject the mocked ValidationManager
        try {
            java.lang.reflect.Field field = ControllerMapper.class.getDeclaredField("validationManager");
            field.setAccessible(true);
            field.set(controllerMapper, validationManager);
        } catch (Exception e) {
            fail("Failed to set up test: " + e.getMessage());
        }

        lenient().when(context.response()).thenReturn(responseHandler);
        lenient().when(context.headers()).thenReturn(headersManager);
    }

    @Test
    void mapControllerRoutes_ShouldMapGetMethod() {
        // Given
        Map<Class<?>, Object> controllers = new HashMap<>();
        TestController controller = new TestController();
        controllers.put(TestController.class, controller);

        // When
        controllerMapper.mapControllerRoutes(app, controllers);

        // Then
        verify(app).get(eq("/test/get"), any());
    }

    @Test
    void mapControllerRoutes_ShouldMapPostMethod() {
        // Given
        Map<Class<?>, Object> controllers = new HashMap<>();
        TestController controller = new TestController();
        controllers.put(TestController.class, controller);

        // When
        controllerMapper.mapControllerRoutes(app, controllers);

        // Then
        verify(app).post(eq("/test/post"), any());
    }

    @Test
    void mapControllerRoutes_ShouldMapPutMethod() {
        // Given
        Map<Class<?>, Object> controllers = new HashMap<>();
        TestController controller = new TestController();
        controllers.put(TestController.class, controller);

        // When
        controllerMapper.mapControllerRoutes(app, controllers);

        // Then
        verify(app).put(eq("/test/put"), any());
    }

    @Test
    void mapControllerRoutes_ShouldMapDeleteMethod() {
        // Given
        Map<Class<?>, Object> controllers = new HashMap<>();
        TestController controller = new TestController();
        controllers.put(TestController.class, controller);

        // When
        controllerMapper.mapControllerRoutes(app, controllers);

        // Then
        verify(app).delete(eq("/test/delete"), any());
    }

    @Test
    void mapControllerRoutes_ShouldMapPatchMethod() {
        // Given
        Map<Class<?>, Object> controllers = new HashMap<>();
        TestController controller = new TestController();
        controllers.put(TestController.class, controller);

        // When
        controllerMapper.mapControllerRoutes(app, controllers);

        // Then
        verify(app).patch(eq("/test/patch"), any());
    }

    @Test
    void mapControllerRoutes_ShouldMapOptionsMethod() {
        // Given
        Map<Class<?>, Object> controllers = new HashMap<>();
        TestController controller = new TestController();
        controllers.put(TestController.class, controller);

        // When
        controllerMapper.mapControllerRoutes(app, controllers);

        // Then
        verify(app).options(eq("/test/options"), any());
    }

    @Test
    void mapControllerRoutes_ShouldMapHeadMethod() {
        // Given
        Map<Class<?>, Object> controllers = new HashMap<>();
        TestController controller = new TestController();
        controllers.put(TestController.class, controller);

        // When
        controllerMapper.mapControllerRoutes(app, controllers);

        // Then
        verify(app).head(eq("/test/head"), any());
    }

    @Test
    void mapControllerRoutes_ShouldHandleEmptyControllers() {
        // Given
        Map<Class<?>, Object> controllers = new HashMap<>();

        // When
        controllerMapper.mapControllerRoutes(app, controllers);

        // Then
        verifyNoInteractions(app);
    }

    @Test
    void mapControllerRoutes_ShouldHandleNullControllers() {
        // When
        controllerMapper.mapControllerRoutes(app, null);

        // Then
        verifyNoInteractions(app);
    }

    @Test
    void combinePaths_ShouldHandleEmptyBasePath() throws Exception {
        // Given
        Method method = ControllerMapper.class.getDeclaredMethod("combinePaths", String.class, String.class);
        method.setAccessible(true);

        // When
        String result = (String) method.invoke(controllerMapper, "", "/path");

        // Then
        assertEquals("/path", result);
    }

    @Test
    void combinePaths_ShouldHandleBasePathWithTrailingSlash() throws Exception {
        // Given
        Method method = ControllerMapper.class.getDeclaredMethod("combinePaths", String.class, String.class);
        method.setAccessible(true);

        // When
        String result = (String) method.invoke(controllerMapper, "/base/", "path");

        // Then
        assertEquals("/base/path", result);
    }

    @Test
    void combinePaths_ShouldHandlePathWithoutLeadingSlash() throws Exception {
        // Given
        Method method = ControllerMapper.class.getDeclaredMethod("combinePaths", String.class, String.class);
        method.setAccessible(true);

        // When
        String result = (String) method.invoke(controllerMapper, "/base", "path");

        // Then
        assertEquals("/base/path", result);
    }

    @Test
    void handleControllerResult_ShouldHandleResponseObject() throws Exception {
        // Given
        Method method = ControllerMapper.class.getDeclaredMethod("handleControllerResult", Object.class, Context.class);
        method.setAccessible(true);
        
        Response<?> response = Response.ok().build();

        // When
        method.invoke(controllerMapper, response, context);

        // Then
        verify(responseHandler).handleResponse(same(response));
    }

    @Test
    void handleControllerResult_ShouldHandleNullResult() throws Exception {
        // Given
        Method method = ControllerMapper.class.getDeclaredMethod("handleControllerResult", Object.class, Context.class);
        method.setAccessible(true);

        // When
        method.invoke(controllerMapper, null, context);

        // Then
        verify(responseHandler).handleResponse(any(Response.class));
    }

    @Test
    void handleControllerResult_ShouldHandleNonResponseObject() throws Exception {
        // Given
        Method method = ControllerMapper.class.getDeclaredMethod("handleControllerResult", Object.class, Context.class);
        method.setAccessible(true);
        
        String result = "test result";

        // When
        method.invoke(controllerMapper, result, context);

        // Then
        ArgumentCaptor<Response> responseCaptor = ArgumentCaptor.forClass(Response.class);
        verify(responseHandler).handleResponse(responseCaptor.capture());
        
        Response<?> response = responseCaptor.getValue();
        assertEquals(HttpStatusCode.OK, response.getStatusCode());
        assertEquals(result, response.getBody());
    }

    @Test
    void handleException_ShouldHandleConstraintViolationException() throws Exception {
        // Given
        Method method = ControllerMapper.class.getDeclaredMethod("handleException", Throwable.class, Context.class);
        method.setAccessible(true);
        
        ConstraintViolationException exception = mock(ConstraintViolationException.class);
        when(exception.getMessage()).thenReturn("Validation failed");

        // When
        method.invoke(controllerMapper, exception, context);

        // Then
        ArgumentCaptor<Response> responseCaptor = ArgumentCaptor.forClass(Response.class);
        verify(responseHandler).handleResponse(responseCaptor.capture());
        
        Response<?> response = responseCaptor.getValue();
        assertEquals(HttpStatusCode.BAD_REQUEST, response.getStatusCode());
        assertInstanceOf(ErrorResponse.class, response.getBody());
    }

    @Test
    void handleException_ShouldUseExceptionHandler() throws Exception {
        // Given
        Field registryField = ControllerMapper.class.getDeclaredField("exceptionHandlerRegistry");
        registryField.setAccessible(true);
        registryField.set(controllerMapper, exceptionHandlerRegistry);

        Method method = ControllerMapper.class.getDeclaredMethod("handleException", Throwable.class, Context.class);
        method.setAccessible(true);

        RuntimeException exception = new RuntimeException("Test exception");
        ExceptionHandlerMethod handlerMethod = mock(ExceptionHandlerMethod.class);
        Response<ErrorResponse> handlerResponse = Response.status(HttpStatusCode.BAD_REQUEST)
                .body(new ErrorResponse(
                        HttpStatusCode.BAD_REQUEST,
                        "Custom handler response",
                        "/test-path",
                        "TestException"))
                .build();

        // Use lenient to avoid UnnecessaryStubbingException
        lenient().when(context.getPath()).thenReturn("/test-path");
        lenient().when(exceptionHandlerRegistry.findHandler(any())).thenReturn(handlerMethod);
        lenient().when(handlerMethod.invoke(any(), eq(context))).thenReturn(handlerResponse);

        // When
        method.invoke(controllerMapper, exception, context);

        // Then
        verify(exceptionHandlerRegistry).findHandler(any());
        verify(handlerMethod).invoke(any(), eq(context));

        ArgumentCaptor<Response> responseCaptor = ArgumentCaptor.forClass(Response.class);
        verify(responseHandler).handleResponse(responseCaptor.capture());
        Response<?> capturedResponse = responseCaptor.getValue();

        assertEquals(HttpStatusCode.BAD_REQUEST, capturedResponse.getStatusCode());
        assertInstanceOf(ErrorResponse.class, capturedResponse.getBody());
        assertEquals("Custom handler response", ((ErrorResponse)capturedResponse.getBody()).getMessage());
    }

    @Test
    void handleException_ShouldHandleGenericException() throws Exception {
        // Given
        Method method = ControllerMapper.class.getDeclaredMethod("handleException", Throwable.class, Context.class);
        method.setAccessible(true);
        
        RuntimeException exception = new RuntimeException("Test exception");
        lenient().when(exceptionHandlerRegistry.findHandler(any())).thenReturn(null);
        lenient().when(context.getPath()).thenReturn("/test/path");

        // When
        method.invoke(controllerMapper, exception, context);

        // Then
        ArgumentCaptor<Response> responseCaptor = ArgumentCaptor.forClass(Response.class);
        verify(responseHandler).handleResponse(responseCaptor.capture());

        Response<?> response = responseCaptor.getValue();
        assertEquals(HttpStatusCode.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody() instanceof ErrorResponse);
    }

    @Test
    void invokeControllerMethod_ShouldSucceedWithNoParameters() throws Exception {
        // Given
        Method method = TestController.class.getDeclaredMethod("getMethod");
        TestController controller = new TestController();

        Method invokeMethod = ControllerMapper.class.getDeclaredMethod("invokeControllerMethod",
                Object.class, Method.class, Context.class);
        invokeMethod.setAccessible(true);

        // When
        Object result = invokeMethod.invoke(controllerMapper, controller, method, context);

        // Then
        assertEquals("get", result);
        verify(responseHandler).handleResponse(any(Response.class));
        verifyNoInteractions(parameterResolver); // No parameters to resolve
    }

    @Test
    void invokeControllerMethod_ShouldResolveParameters() throws Exception {
        // Given
        Method method = TestController.class.getDeclaredMethod("methodWithParam", String.class);
        TestController controller = new TestController();

        Field resolverField = ControllerMapper.class.getDeclaredField("parameterResolver");
        resolverField.setAccessible(true);
        resolverField.set(controllerMapper, parameterResolver);

        Method invokeMethod = ControllerMapper.class.getDeclaredMethod("invokeControllerMethod",
                Object.class, Method.class, Context.class);
        invokeMethod.setAccessible(true);

        when(parameterResolver.resolveParameter(any(), any())).thenReturn("resolvedParam");

        // When
        Object result = invokeMethod.invoke(controllerMapper, controller, method, context);

        // Then
        assertEquals("param:resolvedParam", result);
        verify(parameterResolver).resolveParameter(any(), any());
        verify(validationManager).validateMethodParameters(eq(controller), eq(method), any(), any());
        verify(responseHandler).handleResponse(any());
    }

    @Test
    void invokeControllerMethod_ShouldThrowConstraintViolationException() throws Exception {
        // Given
        Method method = TestController.class.getDeclaredMethod("methodWithParam", String.class);
        TestController controller = new TestController();

        Method invokeMethod = ControllerMapper.class.getDeclaredMethod("invokeControllerMethod",
                Object.class, Method.class, Context.class);
        invokeMethod.setAccessible(true);

        lenient().when(parameterResolver.resolveParameter(any(), any())).thenReturn("resolvedParam");
        ConstraintViolationException cve = new ConstraintViolationException("Validation failed", null);
        doThrow(cve).when(validationManager).validateMethodParameters(any(), any(), any(), any());

        // When/Then
        ConstraintViolationException thrown = assertThrows(ConstraintViolationException.class, () -> {
            try {
                invokeMethod.invoke(controllerMapper, controller, method, context);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
        });

        assertEquals("Validation failed", thrown.getMessage());
    }

    @Test
    void invokeControllerMethod_ShouldHandleMethodException() throws Exception {
        // Given
        Method method = TestController.class.getDeclaredMethod("methodThatThrowsException");
        TestController controller = new TestController();

        Method invokeMethod = ControllerMapper.class.getDeclaredMethod("invokeControllerMethod",
                Object.class, Method.class, Context.class);
        invokeMethod.setAccessible(true);

        // When/Then
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
            try {
                invokeMethod.invoke(controllerMapper, controller, method, context);
            } catch (InvocationTargetException e) {
                if (e.getCause() != null) {
                    throw e.getCause();
                }
                throw e;
            }
        });

        assertEquals("Failed to invoke controller method: methodThatThrowsException", thrown.getMessage());
    }

    @Test
    void invokeControllerMethod_ShouldHandleParameterResolutionException() throws Exception {
        // Given
        Method method = TestController.class.getDeclaredMethod("methodWithParam", String.class);
        TestController controller = new TestController();

        // Manually inject the mock parameterResolver
        Field resolverField = ControllerMapper.class.getDeclaredField("parameterResolver");
        resolverField.setAccessible(true);
        resolverField.set(controllerMapper, parameterResolver);

        Method invokeMethod = ControllerMapper.class.getDeclaredMethod("invokeControllerMethod",
                Object.class, Method.class, Context.class);
        invokeMethod.setAccessible(true);

        RuntimeException resolutionException = new RuntimeException("Parameter resolution failed");
        when(parameterResolver.resolveParameter(any(), any())).thenThrow(resolutionException);

        // When/Then
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
            try {
                invokeMethod.invoke(controllerMapper, controller, method, context);
            } catch (InvocationTargetException e) {
                if (e.getCause() != null) {
                    throw e.getCause();
                }
                throw e;
            }
        });

        assertTrue(thrown.getMessage().contains("Failed to invoke controller method"));
        assertEquals(resolutionException, thrown.getCause());
    }

    @Test
    void handleWithMiddleware_ShouldExecuteMiddlewareAndInvokeControllerMethod() throws Exception {
        // Given
        TestController controller = new TestController();
        Method controllerMethod = TestController.class.getDeclaredMethod("methodWithMiddleware");
        Context mockContext = mock(Context.class);

        // Mock headers and response
        HeadersManager mockHeaders = mock(HeadersManager.class);
        ResponseHandler mockResponseHandler = mock(ResponseHandler.class);
        when(mockContext.headers()).thenReturn(mockHeaders);
        when(mockContext.response()).thenReturn(mockResponseHandler);

        // Access the private method using reflection
        Method handleWithMiddlewareMethod = ControllerMapper.class.getDeclaredMethod(
                "handleWithMiddleware",
                Object.class, Method.class, Context.class, Class[].class);
        handleWithMiddlewareMethod.setAccessible(true);

        // Create middleware array with TestMiddleware
        Class<? extends Middleware>[] controllerMiddleware = new Class[] { TestMiddleware.class };

        // When
        handleWithMiddlewareMethod.invoke(controllerMapper, controller, controllerMethod, mockContext, controllerMiddleware);

        // Then
        verify(mockHeaders).setHeader(eq(RequestHeader.CONTENT_TYPE.getHeaderName()), anyString());
        verify(mockResponseHandler).handleResponse(any(Response.class));
    }

    @Test
    void handleWithMiddleware_ShouldHandleExceptionFromMiddleware() throws Exception {
        // Given
        TestController controller = new TestController();
        Method controllerMethod = TestController.class.getDeclaredMethod("methodWithMiddleware");
        Context mockContext = mock(Context.class);

        // Mock headers and response
        HeadersManager mockHeaders = mock(HeadersManager.class);
        ResponseHandler mockResponseHandler = mock(ResponseHandler.class);
        when(mockContext.headers()).thenReturn(mockHeaders);
        when(mockContext.response()).thenReturn(mockResponseHandler);
        when(mockContext.getPath()).thenReturn("/test/with-middleware");

        // Access the private method using reflection
        Method handleWithMiddlewareMethod = ControllerMapper.class.getDeclaredMethod(
                "handleWithMiddleware",
                Object.class, Method.class, Context.class, Class[].class);
        handleWithMiddlewareMethod.setAccessible(true);

        // Create middleware array with the failing middleware
        Class<? extends Middleware>[] controllerMiddleware = new Class[] { TestMiddleware.class };

        // When
        handleWithMiddlewareMethod.invoke(controllerMapper, controller, controllerMethod, mockContext, controllerMiddleware);

        // Then
        // Verify that the exception is properly handled
        ArgumentCaptor<Response<?>> responseCaptor = ArgumentCaptor.forClass(Response.class);
        verify(mockResponseHandler).handleResponse(responseCaptor.capture());

        Response<?> capturedResponse = responseCaptor.getValue();
        ErrorResponse errorResponse = (ErrorResponse)capturedResponse.getBody();
        assertEquals(HttpStatusCode.INTERNAL_SERVER_ERROR, errorResponse.getStatus());
        assertTrue(errorResponse.getMessage().contains("Middleware failure"));
    }

    @Test
    void handleWithMiddleware_ShouldHandleContentNegotiationFailure() throws Exception {
        // Given
        TestController controller = new TestController();
        Method controllerMethod = TestController.class.getDeclaredMethod("methodWithMiddleware");
        Context mockContext = mock(Context.class);

        // Mock headers and response
        HeadersManager mockHeaders = mock(HeadersManager.class);
        ResponseHandler mockResponseHandler = mock(ResponseHandler.class);
        lenient().when(mockContext.headers()).thenReturn(mockHeaders);
        when(mockContext.response()).thenReturn(mockResponseHandler);
        when(mockContext.getPath()).thenReturn("/test/with-middleware");

        // Override contentNegotiationManager to throw exception
        Field cnmField = ControllerMapper.class.getDeclaredField("contentNegotiationManager");
        cnmField.setAccessible(true);
        ContentNegotiationManager mockCNM = mock(ContentNegotiationManager.class);
        doThrow(new RuntimeException("Content type not supported"))
                .when(mockCNM).validateContentType(any(), any());
        cnmField.set(controllerMapper, mockCNM);

        // Access the private method using reflection
        Method handleWithMiddlewareMethod = ControllerMapper.class.getDeclaredMethod(
                "handleWithMiddleware",
                Object.class, Method.class, Context.class, Class[].class);
        handleWithMiddlewareMethod.setAccessible(true);

        // When
        handleWithMiddlewareMethod.invoke(controllerMapper, controller, controllerMethod, mockContext, new Class[0]);

        // Then
        ArgumentCaptor<Response<?>> responseCaptor = ArgumentCaptor.forClass(Response.class);
        verify(mockResponseHandler).handleResponse(responseCaptor.capture());

        Response<?> capturedResponse = responseCaptor.getValue();
        ErrorResponse errorResponse = (ErrorResponse)capturedResponse.getBody();
        assertEquals(HttpStatusCode.INTERNAL_SERVER_ERROR, errorResponse.getStatus());
        assertTrue(errorResponse.getMessage().contains("Content type not supported"));
    }

    @Test
    void handleWithMiddleware_ShouldHandleConstraintViolationException() throws Exception {
        // Given
        TestController controller = new TestController();
        Method controllerMethod = TestController.class.getDeclaredMethod("methodWithParam", String.class);
        Context mockContext = mock(Context.class);

        // Mock headers and response
        HeadersManager mockHeaders = mock(HeadersManager.class);
        ResponseHandler mockResponseHandler = mock(ResponseHandler.class);
        when(mockContext.headers()).thenReturn(mockHeaders);
        when(mockContext.response()).thenReturn(mockResponseHandler);

        // Set up the validation manager to throw constraint violation
        ValidationManager mockValidator = mock(ValidationManager.class);
        doThrow(new ConstraintViolationException("Validation failed", null))
                .when(mockValidator).validateMethodParameters(any(), any(), any(), any());

        Field validatorField = ControllerMapper.class.getDeclaredField("validationManager");
        validatorField.setAccessible(true);
        validatorField.set(controllerMapper, mockValidator);

        // Make sure parameterResolver returns a value
        when(parameterResolver.resolveParameter(any(), any())).thenReturn("test");

        Field resolverField = ControllerMapper.class.getDeclaredField("parameterResolver");
        resolverField.setAccessible(true);
        resolverField.set(controllerMapper, parameterResolver);

        // Access the private method using reflection
        Method handleWithMiddlewareMethod = ControllerMapper.class.getDeclaredMethod(
                "handleWithMiddleware",
                Object.class, Method.class, Context.class, Class[].class);
        handleWithMiddlewareMethod.setAccessible(true);

        // When
        handleWithMiddlewareMethod.invoke(controllerMapper, controller, controllerMethod, mockContext, new Class[0]);

        // Then
        ArgumentCaptor<Response<?>> responseCaptor = ArgumentCaptor.forClass(Response.class);
        verify(mockResponseHandler).handleResponse(responseCaptor.capture());

        Response<?> capturedResponse = responseCaptor.getValue();
        assertEquals(HttpStatusCode.BAD_REQUEST, capturedResponse.getStatusCode());
    }

    // Test controller for route mapping tests
    @Controller("/test")
    public static class TestController {
        @Get("/get")
        public String getMethod() {
            return "get";
        }

        @Post("/post")
        public String postMethod() {
            return "post";
        }

        @Put("/put")
        public String putMethod() {
            return "put";
        }

        @Delete("/delete")
        public String deleteMethod() {
            return "delete";
        }

        @Patch("/patch")
        public String patchMethod() {
            return "patch";
        }

        @Options("/options")
        public String optionsMethod() {
            return "options";
        }

        @Head("/head")
        public String headMethod() {
            return "head";
        }

        @Get("/with-middleware")
        @WithMiddleware({TestMiddleware.class})
        public String methodWithMiddleware() {
            return "middleware";
        }

        @Get("/validated")
        public String validatedMethod(@Validated TestDTO dto) {
            return "validated";
        }

        @Get("/method-with-param/:param")
        public String methodWithParam(@PathParameter("param") String param) {
            return "param:" + param;
        }

        @Get("/method-that-throws-exception")
        public String methodThatThrowsException() {
            throw new RuntimeException("This method throws exception");
        }
    }

    // Test middleware for middleware tests
    public static class TestMiddleware implements Middleware {
        @Override
        public void handle(Context context) {
            throw new RuntimeException("Middleware failure");
        }
    }

    public record TestDTO(
            @NotNull String testName
    ){}
}