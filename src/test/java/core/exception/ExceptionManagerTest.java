package core.exception;

import io.github.renatompf.ember.core.exception.ExceptionHandlerMethod;
import io.github.renatompf.ember.core.exception.ExceptionManager;
import io.github.renatompf.ember.core.http.Response;
import io.github.renatompf.ember.core.server.Context;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

class ExceptionManagerTest {

    private ExceptionManager exceptionManager;
    private Context mockContext;
    private ExceptionHandlerMethod mockHandler;

    @BeforeEach
    void setUp() {
        exceptionManager = new ExceptionManager();
        mockContext = mock(Context.class);
        mockHandler = mock(ExceptionHandlerMethod.class);
    }

    @Test
    void testRegisterHandler_and_handleException_successfullyInvokesHandler() throws Exception {
        // Arrange
        RuntimeException exception = new RuntimeException("Test error");
        Response<?> expectedResponse = mock(Response.class);

        when(mockHandler.invoke(exception, mockContext)).thenReturn(expectedResponse);
        exceptionManager.registerHandler(RuntimeException.class, mockHandler);

        // Act
        Response<?> response = exceptionManager.handleException(exception, mockContext);

        // Assert
        assertEquals(expectedResponse, response);
        verify(mockHandler).invoke(exception, mockContext);
    }

    @Test
    void testHandleException_noHandler_returnsNull() {
        // Arrange
        IllegalArgumentException exception = new IllegalArgumentException("No handler registered");

        // Act
        Response<?> response = exceptionManager.handleException(exception, mockContext);

        // Assert
        assertNull(response);
    }

    @Test
    void testHandleException_handlerThrowsException_logsErrorAndReturnsNull() throws Exception {
        // Arrange
        RuntimeException exception = new RuntimeException("Boom");

        doThrow(new RuntimeException("Handler failed"))
                .when(mockHandler).invoke(exception, mockContext);

        exceptionManager.registerHandler(RuntimeException.class, mockHandler);

        // Act
        Response<?> response = exceptionManager.handleException(exception, mockContext);

        // Assert
        assertNull(response);
        verify(mockHandler).invoke(exception, mockContext);
    }

    @Test
    void testUnwrapException_delegatesToCause_whenWrappedInRuntimeException() throws Exception {
        // Arrange
        IllegalStateException rootCause = new IllegalStateException("Deep cause");
        RuntimeException wrapped = new RuntimeException(rootCause);
        ExceptionHandlerMethod handler = mock(ExceptionHandlerMethod.class);
        exceptionManager.registerHandler(IllegalStateException.class, handler);

        // Act
        exceptionManager.handleException(wrapped, mockContext);

        // Assert
        verify(handler).invoke(rootCause, mockContext);
    }
}