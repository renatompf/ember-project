package io.github.renatompf.ember.core.controller;

import io.github.renatompf.ember.EmberApplication;
import io.github.renatompf.ember.annotations.controller.Controller;
import io.github.renatompf.ember.annotations.http.*;
import io.github.renatompf.ember.annotations.middleware.WithMiddleware;
import io.github.renatompf.ember.core.exception.ExceptionHandlerMethod;
import io.github.renatompf.ember.core.exception.ExceptionHandlerRegistry;
import io.github.renatompf.ember.core.http.ErrorResponse;
import io.github.renatompf.ember.core.http.Response;
import io.github.renatompf.ember.core.parameter.ContentNegotiationManager;
import io.github.renatompf.ember.core.parameter.ParameterResolver;
import io.github.renatompf.ember.core.server.Context;
import io.github.renatompf.ember.core.server.Middleware;
import io.github.renatompf.ember.core.validation.ValidationManager;
import io.github.renatompf.ember.enums.HttpStatusCode;
import io.github.renatompf.ember.enums.MediaType;
import io.github.renatompf.ember.enums.RequestHeader;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Map;
import java.util.function.Consumer;

/**
 * The `ControllerMapper` class is responsible for mapping controller routes to the Ember application.
 * It handles HTTP method annotations, middleware execution, parameter resolution, and exception handling.
 */
public class ControllerMapper {
    private static final Logger logger = LoggerFactory.getLogger(ControllerMapper.class);
    
    private final ExceptionHandlerRegistry exceptionHandlerRegistry;
    private final ContentNegotiationManager contentNegotiationManager;
    private final ParameterResolver parameterResolver;
    private final ValidationManager validationManager;

    /**
     * Constructor for ControllerMapper.
     *
     * @param exceptionHandlerRegistry Registry for exception handlers
     * @param parameterResolver Parameter resolver for method parameters
     */
    public ControllerMapper(ExceptionHandlerRegistry exceptionHandlerRegistry,
                           ParameterResolver parameterResolver) {
        this.exceptionHandlerRegistry = exceptionHandlerRegistry;
        this.contentNegotiationManager = new ContentNegotiationManager();
        this.validationManager = new ValidationManager();
        this.parameterResolver = parameterResolver;
    }

    /**
     * Maps controller routes to the Ember application.
     *
     * @param app The Ember application
     * @param controllers A map of controller classes to their instances
     */
    public void mapControllerRoutes(EmberApplication app, Map<Class<?>, Object> controllers) {
        if (controllers == null || controllers.isEmpty()) {
            logger.warn("No controllers registered to map routes for");
            return;
        }

        for (Object controller : controllers.values()) {
            if (controller == null) {
                continue;
            }

            Class<?> clazz = controller.getClass();
            if (clazz.isAnnotationPresent(Controller.class)) {
                String basePath = clazz.getAnnotation(Controller.class).value();
                logger.info("Mapping routes for controller: {}", clazz.getName());

                // Collect controller-level middleware
                Class<? extends Middleware>[] controllerMiddleware = clazz.isAnnotationPresent(WithMiddleware.class)
                        ? clazz.getAnnotation(WithMiddleware.class).value()
                        : new Class[0];

                for (Method method : clazz.getDeclaredMethods()) {
                    mapHttpMethod(app, controller, method, basePath, controllerMiddleware);
                }
            }
        }
    }

    /**
     * Maps HTTP method annotations to the Ember application routes.
     *
     * @param app The Ember application
     * @param controller The controller instance
     * @param method The method to map
     * @param basePath The base path of the controller
     * @param controllerMiddleware Array of controller-level middleware classes
     */
    private void mapHttpMethod(EmberApplication app, Object controller, Method method, String basePath, 
                             Class<? extends Middleware>[] controllerMiddleware) {
        Consumer<String> registerRoute = null;
        String path = null;
        
        if (method.isAnnotationPresent(Get.class)) {
            path = combinePaths(basePath, method.getAnnotation(Get.class).value());
            registerRoute = p -> app.get(p, ctx -> handleWithMiddleware(controller, method, ctx, controllerMiddleware));
        } else if (method.isAnnotationPresent(Post.class)) {
            path = combinePaths(basePath, method.getAnnotation(Post.class).value());
            registerRoute = p -> app.post(p, ctx -> handleWithMiddleware(controller, method, ctx, controllerMiddleware));
        } else if (method.isAnnotationPresent(Put.class)) {
            path = combinePaths(basePath, method.getAnnotation(Put.class).value());
            registerRoute = p -> app.put(p, ctx -> handleWithMiddleware(controller, method, ctx, controllerMiddleware));
        } else if (method.isAnnotationPresent(Delete.class)) {
            path = combinePaths(basePath, method.getAnnotation(Delete.class).value());
            registerRoute = p -> app.delete(p, ctx -> handleWithMiddleware(controller, method, ctx, controllerMiddleware));
        } else if (method.isAnnotationPresent(Patch.class)) {
            path = combinePaths(basePath, method.getAnnotation(Patch.class).value());
            registerRoute = p -> app.patch(p, ctx -> handleWithMiddleware(controller, method, ctx, controllerMiddleware));
        } else if (method.isAnnotationPresent(Options.class)) {
            path = combinePaths(basePath, method.getAnnotation(Options.class).value());
            registerRoute = p -> app.options(p, ctx -> handleWithMiddleware(controller, method, ctx, controllerMiddleware));
        } else if (method.isAnnotationPresent(Head.class)) {
            path = combinePaths(basePath, method.getAnnotation(Head.class).value());
            registerRoute = p -> app.head(p, ctx -> handleWithMiddleware(controller, method, ctx, controllerMiddleware));
        }
        
        if (registerRoute != null && path != null) {
            registerRoute.accept(path);
            logger.debug("Mapped {} route: {}", method.getName(), path);
        }
    }

    /**
     * Combines a base path with a relative path.
     *
     * @param base The base path
     * @param path The relative path
     * @return The combined path
     */
    private String combinePaths(String base, String path) {
        logger.debug("Combining paths: {} + {}", base, path);
        if (base.isEmpty()) {
            return path.startsWith("/") ? path : "/" + path;
        }
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return base + path;
    }

    /**
     * Handles middleware execution and invokes the controller method.
     *
     * @param controller The controller instance
     * @param method The method to invoke
     * @param context The request context
     * @param controllerMiddleware Array of controller-level middleware classes
     */
    private void handleWithMiddleware(Object controller, Method method, Context context,
                                     Class<? extends Middleware>[] controllerMiddleware) {
        try {
            logger.debug("Handling middleware for method: {}.{}", controller.getClass().getName(), method.getName());
            
            // Collect method-level middleware
            Class<? extends Middleware>[] methodMiddleware = method.isAnnotationPresent(WithMiddleware.class)
                    ? method.getAnnotation(WithMiddleware.class).value()
                    : new Class[0];

            // Validate content type
            contentNegotiationManager.validateContentType(context, method);
            MediaType responseType = contentNegotiationManager.negotiateResponseType(context, method);
            context.headers().setHeader(RequestHeader.CONTENT_TYPE.getHeaderName(), responseType.getType());

            // Execute controller-level middleware
            for (Class<? extends Middleware> middlewareClass : controllerMiddleware) {
                logger.debug("Executing controller-level middleware: {}", middlewareClass.getName());
                Middleware middlewareInstance = middlewareClass.getDeclaredConstructor().newInstance();
                middlewareInstance.handle(context);
            }

            // Execute method-level middleware
            for (Class<? extends Middleware> middlewareClass : methodMiddleware) {
                logger.debug("Executing method-level middleware: {}", middlewareClass.getName());
                Middleware middlewareInstance = middlewareClass.getDeclaredConstructor().newInstance();
                middlewareInstance.handle(context);
            }

            // Invoke the controller method
            invokeControllerMethod(controller, method, context);
        } catch (Exception e) {
            handleException(e, context);
        }
    }

    /**
     * Invokes a controller method with resolved parameters.
     *
     * @param controller The controller instance
     * @param method The method to invoke
     * @param context The request context
     * @return The result of the method invocation
     */
    private Object invokeControllerMethod(Object controller, Method method, Context context) {
        try {
            logger.debug("Invoking controller method: {}.{}", controller.getClass().getName(), method.getName());
            Parameter[] parameters = method.getParameters();
            Object[] args = new Object[parameters.length];

            for (int i = 0; i < parameters.length; i++) {
                Parameter parameter = parameters[i];
                args[i] = parameterResolver.resolveParameter(parameter, context);
                logger.debug("Resolved parameter {} of method {}.{} to {}", 
                        i, controller.getClass().getName(), method.getName(), args[i]);
            }

            validationManager.validateMethodParameters(controller, method, parameters, args);

            try {
                Object result = method.invoke(controller, args);
                logger.debug("Controller method {}.{} returned: {}", 
                        controller.getClass().getName(), method.getName(), result);
                handleControllerResult(result, context);
                return result;
            } catch (InvocationTargetException e) {
                // Unwrap the actual exception thrown by the method
                throw e.getCause();
            }
        } catch (ConstraintViolationException e){
            throw e;
        } catch (Throwable e) {
            logger.error("Failed to invoke controller method: {}.{} - {}", 
                    controller.getClass().getName(), method.getName(), e.getMessage());
            throw new RuntimeException("Failed to invoke controller method: " + method.getName(), e);
        }
    }

    /**
     * Handles the result of a controller method and sends the response.
     *
     * @param result The result of the controller method
     * @param context The request context
     */
    private void handleControllerResult(Object result, Context context) {
        Response<?> response = convertToResponse(result);
        logger.debug("Handling response with status code {} and body: {}", 
                response.getStatusCode(), response.getBody());
        context.response().handleResponse(response);
    }

    /**
     * Converts a result to a `Response` object.
     *
     * @param result The result to convert
     * @return A `Response` object
     */
    private Response<?> convertToResponse(Object result) {
        if (result instanceof Response<?>) {
            return (Response<?>) result;
        }

        if (result == null) {
            return Response.ok().build();
        }

        return Response.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(result)
                .build();
    }

    /**
     * Handles exceptions during controller method execution.
     *
     * @param e The exception thrown
     * @param context The request context
     */
    private void handleException(Throwable e, Context context) {
        logger.debug("Handling exception: {}", e.getMessage());

        // Unwrap any RuntimeException to get the actual cause
        Throwable actualException = e;
        while (actualException instanceof RuntimeException && actualException.getCause() != null) {
            actualException = actualException.getCause();
        }

        if (exceptionHandlerRegistry != null) {
            try {
                ExceptionHandlerMethod handler = exceptionHandlerRegistry.findHandler(actualException);
                if (handler != null) {
                    Object result = handler.invoke(actualException, context);
                    if (result != null) {
                        Response<?> response = convertToResponse(result);
                        context.response().handleResponse(response);
                        return;
                    }
                }
            } catch (Exception handlerException) {
                logger.error("Error in exception handler", handlerException);
            }
        }

        if (e instanceof ConstraintViolationException) {
            context.response().handleResponse(
                    Response.status(HttpStatusCode.BAD_REQUEST)
                            .body(new ErrorResponse(
                                    HttpStatusCode.BAD_REQUEST,
                                    "Validation error: " + e.getMessage()
                            )).build());
            return;
        }

        // Default error handling
        context.response().handleResponse(
                Response.status(HttpStatusCode.INTERNAL_SERVER_ERROR)
                        .body(new ErrorResponse(
                                HttpStatusCode.INTERNAL_SERVER_ERROR,
                                actualException.getMessage(),
                                context.getPath(),
                                actualException.getClass().getName()))
                        .build()
        );
    }
}