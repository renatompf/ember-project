package io.github.renatompf.ember.core;

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

    public ExceptionHandlerMethod(Object handler, Method method) {
        this.handler = handler;
        this.method = method;
        this.method.setAccessible(true);
    }

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
