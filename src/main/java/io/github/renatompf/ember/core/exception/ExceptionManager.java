package io.github.renatompf.ember.core.exception;

import io.github.renatompf.ember.core.http.Response;
import io.github.renatompf.ember.core.server.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages exception handling in the Ember framework.
 * <p>
 * This class is responsible for registering exception handlers and invoking them
 * when exceptions occur during request processing.
 * </p>
 */
public class ExceptionManager {
    private static final Logger logger = LoggerFactory.getLogger(ExceptionManager.class);
    private final Map<Class<? extends Throwable>, ExceptionHandlerMethod> handlers = new HashMap<>();

    /**
     * Registers an exception handler for a specific exception type.
     *
     * @param exceptionType The type of exception to handle.
     * @param handler       The handler method to invoke for the exception.
     */
    public void registerHandler(Class<? extends Throwable> exceptionType, ExceptionHandlerMethod handler) {
        handlers.put(exceptionType, handler);
        logger.info("Registered handler for exception: {}", exceptionType.getName());
    }

    /**
     * Finds and invokes the appropriate exception handler for the given exception.
     *
     * @param exception The exception to handle.
     * @param context   The HTTP request context.
     * @return A `Response` object if a handler is found, otherwise `null`.
     */
    public Response<?> handleException(Throwable exception, Context context) {
        Throwable actualException = unwrapException(exception);
        ExceptionHandlerMethod handler = findHandler(actualException.getClass());

        if (handler != null) {
            try {
                logger.debug("Invoking handler for exception: {}", actualException.getClass().getName());
                return (Response<?>) handler.invoke(actualException, context);
            } catch (Exception e) {
                logger.error("Error while invoking exception handler", e);
            }
        }

        logger.warn("No handler found for exception: {}", actualException.getClass().getName());
        return null;
    }

    /**
     * Finds a registered handler for the given exception type.
     *
     * @param exceptionType The type of exception.
     * @return The handler method, or `null` if no handler is registered.
     */
    private ExceptionHandlerMethod findHandler(Class<? extends Throwable> exceptionType) {
        return handlers.getOrDefault(exceptionType, null);
    }

    /**
     * Unwraps nested exceptions to find the root cause.
     *
     * @param exception The exception to unwrap.
     * @return The root cause of the exception.
     */
    private Throwable unwrapException(Throwable exception) {
        while (exception instanceof RuntimeException && exception.getCause() != null) {
            exception = exception.getCause();
        }
        return exception;
    }
}