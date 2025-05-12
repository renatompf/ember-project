package io.github.renatompf.ember.core.exception;

import io.github.renatompf.ember.core.server.Context;

import java.lang.reflect.Method;

/**
 * Represents a method that handles exceptions in a controller advice.
 * <p>
 * This class encapsulates the method and the controller advice instance,
 * allowing for invocation of the method with the appropriate exception.
 * </p>
 */
public class ExceptionHandlerMethod {
    private final Object handler;
    private final Method method;

    /**
     * Constructs an ExceptionHandlerMethod with the specified handler and method.
     *
     * @param handler The instance of the controller advice.
     * @param method  The method that handles exceptions.
     */
    public ExceptionHandlerMethod(Object handler, Method method) {
        this.handler = handler;
        this.method = method;
        this.method.setAccessible(true);
    }

    /**
     * Invokes the exception handler method with the given exception and context.
     *
     * @param exception The exception to be handled.
     * @param context   The context in which the exception occurred.
     * @return The result of the method invocation.
     * @throws Exception If an error occurs during method invocation.
     */
    public Object invoke(Throwable exception, Context context) throws Exception {
        try {
            Class<?>[] paramTypes = method.getParameterTypes();
            Object[] args;

            if (paramTypes.length == 2) {
                // If method expects 2 parameters (exception and context)
                args = new Object[]{
                        paramTypes[0].cast(exception),  // Cast exception to expected type
                        context
                };
            } else {
                // If method expects only exception parameter
                args = new Object[]{
                        paramTypes[0].cast(exception)  // Cast exception to expected type
                };
            }

            return method.invoke(handler, args);
        } catch (Exception e) {
            throw new Exception("Failed to invoke exception handler", e);
        }
    }

}
