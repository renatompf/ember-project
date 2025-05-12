package io.github.renatompf.ember.core.exception;

import io.github.renatompf.ember.annotations.exceptions.GlobalHandler;
import io.github.renatompf.ember.annotations.exceptions.Handles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Registry for exception handlers.
 * <p>
 * This class is responsible for registering and finding exception handlers
 * based on the type of exception thrown during request processing.
 * </p>
 * <p>
 * It uses reflection to inspect methods annotated with {@code @Handles} in a
 * global handler class, allowing for dynamic registration of exception handlers.
 * </p>
 */
public class ExceptionHandlerRegistry {
    private static final Logger logger = LoggerFactory.getLogger(ExceptionHandlerRegistry.class);
    private final Map<Class<? extends Throwable>, ExceptionHandlerMethod> handlers = new HashMap<>();
    private final Object globalHandler;

    /**
     * Constructs an ExceptionHandlerRegistry with the specified global handler.
     *
     * @param globalHandler The global handler instance, which must be annotated with {@code @GlobalHandler}.
     * @throws IllegalArgumentException if the provided global handler is not annotated with {@code @GlobalHandler}.
     */
    public ExceptionHandlerRegistry(Object globalHandler) {
        // Check for null before accessing methods on globalHandler
        if (globalHandler != null && !globalHandler.getClass().isAnnotationPresent(GlobalHandler.class)) {
            throw new IllegalArgumentException("Class must be annotated with @GlobalHandler");
        }
        this.globalHandler = globalHandler;

        // Only register handlers if we have a valid global handler
        if (globalHandler != null) {
            registerHandlers();
        }
    }

    /**
     * Registers all handlers found in this registry with the provided exception manager.
     *
     * @param manager The exception manager to register handlers with
     */
    public void registerHandlersWithManager(ExceptionManager manager) {
        if (manager == null) {
            return;
        }

        for (Map.Entry<Class<? extends Throwable>, ExceptionHandlerMethod> entry : this.handlers.entrySet()) {
            manager.registerHandler(entry.getKey(), entry.getValue());
        }

        logger.debug("Registered {} exception handlers with manager", this.handlers.size());
    }

    /**
     * Registers exception handlers by scanning the methods of the global handler class.
     * <p>
     * This method looks for methods annotated with {@code @Handles} and registers them
     * in the internal map, associating each exception type with its corresponding handler method.
     * </p>
     */
    private void registerHandlers() {
        Method[] methods = globalHandler.getClass().getDeclaredMethods();
        for (Method method : methods) {
            Handles annotation = method.getAnnotation(Handles.class);
            if (annotation != null) {
                for (Class<? extends Throwable> exceptionType : annotation.value()) {
                    handlers.put(exceptionType, new ExceptionHandlerMethod(globalHandler, method));
                    logger.info("Registered exception handler for: {}", exceptionType.getName());
                }
            }
        }
    }

    /**
     * Finds the appropriate exception handler for the given exception.
     * <p>
     * This method traverses the class hierarchy of the exception to find a matching handler.
     * If no handler is found, it returns null.
     * </p>
     *
     * @param exception The exception for which to find a handler.
     * @return The corresponding ExceptionHandlerMethod, or null if no handler is found.
     */
    public ExceptionHandlerMethod findHandler(Throwable exception) {
        Class<?> exceptionClass = exception.getClass();
        while (exceptionClass != null) {
            ExceptionHandlerMethod handler = handlers.get(exceptionClass);
            if (handler != null) {
                return handler;
            }
            exceptionClass = exceptionClass.getSuperclass();
        }
        return null;
    }
}
