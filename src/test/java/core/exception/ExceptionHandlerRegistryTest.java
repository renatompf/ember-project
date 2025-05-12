package core.exception;

import io.github.renatompf.ember.annotations.exceptions.GlobalHandler;
import io.github.renatompf.ember.annotations.exceptions.Handles;
import io.github.renatompf.ember.core.exception.ExceptionHandlerMethod;
import io.github.renatompf.ember.core.exception.ExceptionHandlerRegistry;
import io.github.renatompf.ember.core.http.ErrorResponse;
import io.github.renatompf.ember.core.http.Response;
import io.github.renatompf.ember.core.server.Context;
import io.github.renatompf.ember.enums.HttpStatusCode;
import io.github.renatompf.ember.exceptions.HttpException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
public class ExceptionHandlerRegistryTest {
    Context context = mock(Context.class);
    private ExceptionHandlerRegistry registry;
    private TestGlobalHandler globalHandler;

    @BeforeEach
    void setUp() {
        globalHandler = new TestGlobalHandler();
        registry = new ExceptionHandlerRegistry(globalHandler);
    }

    @Test
    void shouldThrowExceptionWhenNotGlobalHandler() {
        class InvalidHandler {
        }

        assertThrows(IllegalArgumentException.class,
                () -> new ExceptionHandlerRegistry(new InvalidHandler()));
    }

    @Test
    void shouldFindHandlerForExactExceptionType() {
        ExceptionHandlerMethod handler = registry.findHandler(new TestException());
        assertNotNull(handler);
    }

    @Test
    void shouldFindHandlerForSubclassException() {
        ExceptionHandlerMethod handler = registry.findHandler(new SpecificTestException());
        assertNotNull(handler);
    }

    @Test
    void shouldReturnNullForUnhandledException() {
        ExceptionHandlerMethod handler = registry.findHandler(new UnhandledException());
        assertNull(handler);
    }

    @Test
    void shouldInvokeHandlerSuccessfully() throws Exception {
        TestException exception = new TestException("test message");
        ExceptionHandlerMethod handler = registry.findHandler(exception);

        Object result = handler.invoke(exception, context);

        assertTrue(result instanceof Response);
        Response<?> response = (Response<?>) result;
        assertEquals(HttpStatusCode.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void shouldInvokeMethodSuccessfully() throws Exception {
        Method handleMethod = TestGlobalHandler.class.getDeclaredMethod("handleTestException", TestException.class);
        ExceptionHandlerMethod handler = new ExceptionHandlerMethod(globalHandler, handleMethod);

        TestException exception = new TestException("test");
        Object result = handler.invoke(exception, context);

        assertNotNull(result);
        assertTrue(result instanceof Response);
    }

    @Test
    void shouldHandleInvocationException() throws Exception {
        Method handleMethod = TestGlobalHandler.class.getDeclaredMethod("handleWithError", TestException.class);
        ExceptionHandlerMethod handler = new ExceptionHandlerMethod(globalHandler, handleMethod);

        assertThrows(Exception.class, () -> handler.invoke(new TestException("test"), context));
    }

    @GlobalHandler
    private static class TestGlobalHandler {

        @Handles(TestException.class)
        public Response<ErrorResponse> handleTestException(TestException ex) {
            return Response.status(HttpStatusCode.BAD_REQUEST)
                    .body(new ErrorResponse(
                            HttpStatusCode.BAD_REQUEST,
                            ex.getMessage(),
                            "/test",
                            ex.getClass().getName()))
                    .build();
        }

        @Handles(HttpException.class)
        public Response<ErrorResponse> handleHttpException(HttpException ex) {
            return Response.status(ex.getStatus())
                    .body(new ErrorResponse(
                            ex.getStatus(),
                            ex.getMessage(),
                            "/test",
                            ex.getClass().getName()))
                    .build();
        }

        public Response<ErrorResponse> handleWithError(TestException ex) {
            throw new RuntimeException("Handler error");
        }
    }

    private static class TestException extends Exception {
        public TestException() {
            super();
        }

        public TestException(String message) {
            super(message);
        }
    }

    private static class SpecificTestException extends TestException {
        public SpecificTestException() {
            super();
        }
    }

    private static class UnhandledException extends Exception {
        public UnhandledException() {
            super();
        }
    }


}
