package core.exception;

import io.github.renatompf.ember.core.exception.ExceptionHandlerMethod;
import io.github.renatompf.ember.core.server.Context;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class ExceptionHandlerMethodTest {

    private Context mockContext;
    private TestHandler handler;

    @BeforeEach
    void setUp() {
        handler = new TestHandler();
        mockContext = mock(Context.class);
    }

    @Test
    void testInvoke_withSingleParameter_callsHandlerMethod() throws Exception {
        // Arrange
        Method method = TestHandler.class.getMethod("handleIllegalArgument", IllegalArgumentException.class);
        ExceptionHandlerMethod handlerMethod = new ExceptionHandlerMethod(handler, method);
        IllegalArgumentException exception = new IllegalArgumentException("Invalid arg");

        // Act
        Object result = handlerMethod.invoke(exception, mockContext);

        // Assert
        assertEquals("handled: Invalid arg", result);
    }

    @Test
    void testInvoke_withTwoParameters_callsHandlerMethod() throws Exception {
        // Arrange
        Method method = TestHandler.class.getMethod("handleWithContext", IllegalArgumentException.class, Context.class);
        ExceptionHandlerMethod handlerMethod = new ExceptionHandlerMethod(handler, method);
        IllegalArgumentException exception = new IllegalArgumentException("Invalid arg");

        // Act
        Object result = handlerMethod.invoke(exception, mockContext);

        // Assert
        assertEquals("handled with context: Invalid arg", result);
    }

    @Test
    void testInvoke_withMismatchedType_throwsException() throws Exception {
        // Arrange
        Method method = TestHandler.class.getMethod("handleIllegalArgument", IllegalArgumentException.class);
        ExceptionHandlerMethod handlerMethod = new ExceptionHandlerMethod(handler, method);
        RuntimeException unrelatedException = new RuntimeException("Wrong type");

        // Act + Assert
        Exception ex = assertThrows(Exception.class, () -> handlerMethod.invoke(unrelatedException, mockContext));
        assertTrue(ex.getMessage().contains("Failed to invoke exception handler"));
    }

    // Test helper class
    static class TestHandler {
        public String handleIllegalArgument(IllegalArgumentException ex) {
            return "handled: " + ex.getMessage();
        }

        public String handleWithContext(IllegalArgumentException ex, Context context) {
            return "handled with context: " + ex.getMessage();
        }
    }
}
