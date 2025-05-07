package io.github.renatompf.ember.core;

import io.github.renatompf.ember.EmberApplication;
import io.github.renatompf.ember.annotations.controller.Controller;
import io.github.renatompf.ember.annotations.exceptions.GlobalHandler;
import io.github.renatompf.ember.annotations.http.*;
import io.github.renatompf.ember.annotations.middleware.WithMiddleware;
import io.github.renatompf.ember.annotations.parameters.PathParameter;
import io.github.renatompf.ember.annotations.parameters.QueryParameter;
import io.github.renatompf.ember.annotations.parameters.RequestBody;
import io.github.renatompf.ember.annotations.service.Service;
import io.github.renatompf.ember.enums.HttpStatusCode;
import io.github.renatompf.ember.enums.MediaType;
import io.github.renatompf.ember.enums.RequestHeader;
import io.github.renatompf.ember.utils.TypeConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.reflect.Modifier.isFinal;
import static java.lang.reflect.Modifier.isStatic;

/**
 * The `DIContainer` class is a simple implementation of a Dependency Injection (DI) container.
 * The container supports automatic resolution of dependencies and prevents circular dependencies.
 */
public class DIContainer {

    /**
     * The base package to scan for service and controller classes.
     * If not set, the container will scan the entire classpath.
     */
    private String basePackage = "";

    /**
     * The exception handler registry for handling exceptions in the application.
     * This registry is used to register and find exception handlers based on the type of exception thrown.
     */
    private ExceptionHandlerRegistry exceptionHandlerRegistry;

    private static final Logger logger = LoggerFactory.getLogger(DIContainer.class);

    // Map to store registered service classes and their instances
    private final Map<Class<?>, Object> instances = new ConcurrentHashMap<>();

    // ThreadLocal to keep track of resolving classes to prevent circular dependencies
    private final ThreadLocal<Set<Class<?>>> resolving = ThreadLocal.withInitial(HashSet::new);

    // Marker object to indicate that a service is registered but not yet resolved
    private static final Object UNRESOLVED = new Object();

    // ContentNegotiationManager to handle content negotiation for HTTP requests and responses
    private final ContentNegotiationManager contentNegotiationManager = new ContentNegotiationManager();

    /**
     * Default constructor to create a DIContainer without a specific base package.
     */
    public DIContainer() {
    }

    /**
     * Constructor to create a DIContainer with a specific base package.
     *
     * @param basePackage The base package to scan for service and controller classes.
     */
    public DIContainer(String basePackage) {
        this.basePackage = basePackage;
    }

    /**
     * Registers a service class in the container.
     * The class must be annotated with `@Service`.
     *
     * @param serviceClass The service class to register.
     * @param <T>          The type of the service class.
     * @throws IllegalArgumentException if the class is not annotated with `@Service`.
     */
    public <T> void register(Class<T> serviceClass) {
        if (serviceClass.isAnnotationPresent(Service.class) ||
                serviceClass.isAnnotationPresent(Controller.class) ||
                serviceClass.isAnnotationPresent(GlobalHandler.class)) {
            instances.put(serviceClass, UNRESOLVED); // Mark as registered
            logger.info("Registered service: {}", serviceClass.getName());
        } else {
            logger.error("Class {} is not annotated with @Service or @Controller", serviceClass.getName());
            throw new IllegalArgumentException("Class " + serviceClass.getName() + " is not annotated with @Service");
        }
    }

    /**
     * Automatically registers all service classes found in the classpath.
     * Only classes annotated with `@Service` are registered.
     *
     * @throws RuntimeException if an error occurs during service registration.
     */
    public void registerServices() {
        try {
            List<Class<?>> serviceClasses = findServices();
            logger.info("Found {} service classes", serviceClasses.size());
            for (Class<?> serviceClass : serviceClasses) {
                register(serviceClass);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to register services", e);
        }
    }

    /**
     * Automatically registers all controller classes found in the classpath.
     * Only classes annotated with `@Controller` are registered.
     *
     * @throws RuntimeException if an error occurs during controller registration.
     */
    public void registerControllers() {
        try {
            List<Class<?>> controllerClasses = findControllers();
            logger.info("Found {} controller classes", controllerClasses.size());
            for (Class<?> controllerClass : controllerClasses) {
                register(controllerClass);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to register controllers", e);
        }
    }

    /**
     * Automatically registers all global handler classes found in the classpath.
     * Only classes annotated with `@GlobalHandler` are registered.
     *
     * @throws RuntimeException if an error occurs during global handler registration.
     */
    public void registerGlobalHandlers() {
        try {
            List<Class<?>> handlerClasses = findHandlers();
            logger.info("Found {} global handler classes", handlerClasses.size());
            for (Class<?> handlerClass : handlerClasses) {
                register(handlerClass);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to register global handlers", e);
        }
    }


    /**
     * Registers exception handlers from the global handler class.
     * This method scans for classes annotated with `@GlobalHandler` and registers their methods
     * as exception handlers in the application.
     */
    public void registerExceptionHandlers() {
        for (Map.Entry<Class<?>, Object> entry : instances.entrySet()) {
            Object instance = entry.getValue();
            if (instance != null && instance.getClass().isAnnotationPresent(GlobalHandler.class)) {
                this.exceptionHandlerRegistry = new ExceptionHandlerRegistry(instance);
                logger.info("Registered exception handlers from: {}", instance.getClass().getName());
                return;  // Once we find a handler, we can return
            }
        }
    }


    /**
     * Resolves all registered services by instantiating them and their dependencies.
     */
    public void resolveAll() {
        for (Class<?> serviceClass : instances.keySet()) {
            resolve(serviceClass);
        }
    }


    /**
     * Resolves a specific service class by creating an instance and injecting its dependencies.
     * This method ensures that circular dependencies are prevented and that only one instance
     * of the service is created.
     *
     * @param <T>          The type of the service class.
     * @param serviceClass The service class to resolve.
     * @return The resolved instance of the service class.
     * @throws IllegalStateException if the service is not registered, has circular dependencies,
     *                               or has more than one public constructor.
     * @throws RuntimeException      if an error occurs during service instantiation.
     */
    @SuppressWarnings("unchecked")
    public <T> T resolve(Class<T> serviceClass) {

        if (!isRegistered(serviceClass)) {
            throw new IllegalStateException("Service not registered: " + serviceClass.getName());
        }

        // Check if the service is already resolved
        Object instance = instances.get(serviceClass);
        if (instance != null && instance != UNRESOLVED) {
            logger.debug("Resolved service: {}", serviceClass.getName());
            return (T) instance;
        }

        // Detect circular dependencies and return a partially initialized instance
        if (resolving.get().contains(serviceClass)) {
            logger.debug("Circular dependency detected, returning partially initialized instance for: {}", serviceClass.getName());
            return (T) instances.get(serviceClass);
        }

        // Mark the service as being resolved
        resolving.get().add(serviceClass);
        try {
            logger.debug("Resolving service: {}", serviceClass.getName());
            return (T) instances.computeIfPresent(serviceClass, (cls, existing) -> {
                if (existing != UNRESOLVED) {
                    return existing;
                }

                try {
                    Constructor<?>[] constructors = cls.getConstructors();

                    // Handle classes with no public constructors
                    if (constructors.length == 0) {
                        Field[] fields = cls.getDeclaredFields();
                        boolean hasFieldsToInject = Arrays.stream(fields)
                                .anyMatch(field -> isFinal(field.getModifiers()) &&
                                        !isStatic(field.getModifiers()));

                        if (!hasFieldsToInject) {
                            // Create an instance using the default constructor
                            Constructor<?> constructor = cls.getDeclaredConstructor();
                            constructor.setAccessible(true);
                            T newInstance = (T) constructor.newInstance();
                            instances.put(serviceClass, newInstance);
                            logger.info("Successfully resolved service with default constructor (no dependencies): {}", cls.getName());
                            return newInstance;

                        } else {
                            logger.error("Service {} has fields requiring injection but no public constructor", cls.getName());
                            throw new IllegalStateException("Service " + cls.getName() + " has fields requiring injection but no public constructor");
                        }
                    }

                    Constructor<?> constructor = null;
                    Constructor<?> noArgConstructor = null;

                    for (Constructor<?> ctor : constructors) {
                        if (ctor.getParameterCount() == 0) {
                            noArgConstructor = ctor;
                        } else {
                            constructor = ctor;
                            break;
                        }
                    }

                    // Use constructor with dependencies if available, otherwise use no-arg constructor
                    if (constructor == null) {
                        constructor = noArgConstructor;
                    }

                    Class<?>[] paramTypes = constructor.getParameterTypes();

                    // Handle constructors with no parameters
                    if (paramTypes.length == 0) {
                        T newInstance = (T) constructor.newInstance();
                        instances.put(serviceClass, newInstance);
                        logger.info("Successfully resolved service with no dependencies: {}", cls.getName());
                        return newInstance;
                    }

                    // Handle constructors with dependencies
                    T newInstance = (T) constructor.newInstance(new Object[paramTypes.length]);
                    instances.put(serviceClass, newInstance);

                    // Resolve dependencies for the constructor parameters
                    Object[] dependencies = new Object[paramTypes.length];
                    for (int i = 0; i < paramTypes.length; i++) {
                        dependencies[i] = resolve(paramTypes[i]);
                    }

                    // Inject dependencies into final fields
                    Field[] fields = cls.getDeclaredFields();
                    for (Field field : fields) {
                        if (isFinal(field.getModifiers())) {
                            field.setAccessible(true);
                            for (int i = 0; i < paramTypes.length; i++) {
                                if (field.getType().isAssignableFrom(paramTypes[i])) {
                                    field.set(newInstance, dependencies[i]);
                                    break;
                                }
                            }
                        }
                    }

                    logger.info("Successfully resolved service: {}", cls.getName());
                    return newInstance;

                } catch (Exception e) {
                    logger.error("Failed to resolve service: {}", cls.getName(), e);
                    throw new RuntimeException("Failed to resolve service: " + cls.getName() + " . Message: " + e.getMessage(), e);
                }
            });
        } finally {
            // Remove the service from the resolving set to prevent memory leaks
            resolving.get().remove(serviceClass);
        }
    }

    /**
     * Maps controller routes to the Ember application.
     * This method scans for methods annotated with HTTP method annotations
     * and registers them as routes in the provided Ember application.
     *
     * @param app The Ember application to map routes to.
     */
    public void mapControllerRoutes(EmberApplication app) {
        if (instances == null || instances.isEmpty()) {
            logger.warn("No controllers registered to map routes for");
            return;
        }

        for (Object controller : instances.values()) {
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
                    if (method.isAnnotationPresent(Get.class)) {
                        String path = combinePaths(basePath, method.getAnnotation(Get.class).value());
                        app.get(path, ctx -> handleWithMiddleware(controller, method, ctx, controllerMiddleware));
                        logger.debug("Mapped GET route: {}", path);
                    } else if (method.isAnnotationPresent(Post.class)) {
                        String path = combinePaths(basePath, method.getAnnotation(Post.class).value());
                        app.post(path, ctx -> handleWithMiddleware(controller, method, ctx, controllerMiddleware));
                        logger.debug("Mapped POST route: {}", path);
                    } else if (method.isAnnotationPresent(Put.class)) {
                        String path = combinePaths(basePath, method.getAnnotation(Put.class).value());
                        app.put(path, ctx -> handleWithMiddleware(controller, method, ctx, controllerMiddleware));
                        logger.debug("Mapped PUT route: {}", path);
                    } else if (method.isAnnotationPresent(Delete.class)) {
                        String path = combinePaths(basePath, method.getAnnotation(Delete.class).value());
                        app.delete(path, ctx -> handleWithMiddleware(controller, method, ctx, controllerMiddleware));
                        logger.debug("Mapped DELETE route: {}", path);
                    } else if (method.isAnnotationPresent(Patch.class)) {
                        String path = combinePaths(basePath, method.getAnnotation(Patch.class).value());
                        app.patch(path, ctx -> handleWithMiddleware(controller, method, ctx, controllerMiddleware));
                        logger.debug("Mapped PATCH route: {}", path);
                    } else if (method.isAnnotationPresent(Options.class)) {
                        String path = combinePaths(basePath, method.getAnnotation(Options.class).value());
                        app.options(path, ctx -> handleWithMiddleware(controller, method, ctx, controllerMiddleware));
                        logger.debug("Mapped OPTIONS route: {}", path);
                    } else if (method.isAnnotationPresent(Head.class)) {
                        String path = combinePaths(basePath, method.getAnnotation(Head.class).value());
                        app.head(path, ctx -> handleWithMiddleware(controller, method, ctx, controllerMiddleware));
                        logger.debug("Mapped HEAD route: {}", path);
                    }
                }
            }
        }
    }

    /**
     * Combines a base path with a relative path.
     *
     * @param base The base path.
     * @param path The relative path to combine with the base.
     * @return The combined path.
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
        logger.debug("Combined paths: {} + {} = {}", base, path, base + path);
        return base + path;
    }

    /**
     * Handles the execution of middleware and invokes the corresponding controller method.
     *
     * @param controller           The controller instance containing the method to invoke.
     * @param method               The method to invoke on the controller.
     * @param context              The HTTP request context, which provides access to request data.
     * @param controllerMiddleware An array of middleware classes to execute at the controller level.
     */
    private void handleWithMiddleware(Object controller, Method method, Context context, Class<? extends Middleware>[] controllerMiddleware) {
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
     * Handles exceptions that occur during the execution of controller methods.
     * This method attempts to find a registered exception handler for the given exception
     * and invokes it if found. If no handler is found, a default error response is sent.
     *
     * @param e       The exception that occurred.
     * @param context The HTTP request context used to send the response.
     */
    private void handleException(Throwable e, Context context) {
        logger.debug("Handling exception: {}", e.getMessage());

        // First, unwrap any RuntimeException to get the actual cause
        Throwable actualException = e;
        while (actualException instanceof RuntimeException && actualException.getCause() != null) {
            actualException = actualException.getCause();
        }

        if (exceptionHandlerRegistry != null) {
            logger.debug("Exception handler registry: {}", exceptionHandlerRegistry.getClass().getName());
            try {
                ExceptionHandlerMethod handler = exceptionHandlerRegistry.findHandler(actualException);
                if (handler != null) {
                    logger.debug("Found exception handler: {}", handler);
                    Object result = handler.invoke(actualException, context);
                    logger.debug("Invoking exception handler with result: {}", result);
                    if (result != null) {
                        logger.debug("Handling exception with result: {}", result);
                        Response<?> response = convertToResponse(result);
                        context.response().handleResponse(response);
                        return;
                    }
                    logger.debug("No result from exception handler, sending default error response");
                }
            } catch (Exception handlerException) {
                logger.error("Error in exception handler", handlerException);
            }
        }

        // Default error handling if no specific handler is found
        logger.debug("No exception handler found, sending default error response");
        context.response().handleResponse(
                Response.status(HttpStatusCode.INTERNAL_SERVER_ERROR)
                        .body(new ErrorResponse(
                                HttpStatusCode.INTERNAL_SERVER_ERROR,
                                actualException.getMessage(),  // Use the actual exception message
                                context.getPath(),
                                actualException.getClass().getName()))  // Use the actual exception class
                        .build()
        );
    }


    /**
     * Invokes a controller method with the provided context.
     * This method resolves method parameters, including path parameters,
     * query parameters, and the `Context` object, and invokes the controller method.
     *
     * @param controller The controller instance containing the method to invoke.
     * @param method     The method to invoke on the controller.
     * @param context    The HTTP request context, which provides access to path parameters,
     *                   query parameters, and other request-related data.
     * @return The result of the invoked method, or `null` if the method has a `void` return type.
     * @throws IllegalArgumentException If a required path or query parameter is missing.
     * @throws RuntimeException         If the method invocation fails or an error occurs during parameter resolution.
     */
    private Object invokeControllerMethod(Object controller, Method method, Context context) {
        try {
            logger.debug("Invoking controller method: {}.{}", controller.getClass().getName(), method.getName());
            Parameter[] parameters = method.getParameters();
            Object[] args = new Object[parameters.length];

            for (int i = 0; i < parameters.length; i++) {
                Parameter parameter = parameters[i];
                args[i] = resolveParameter(parameter, context);
                logger.debug("Resolved parameter {} of method {}.{} to {}", i, controller.getClass().getName(), method.getName(), args[i]);
            }

            try {
                Object result = method.invoke(controller, args);
                logger.debug("Controller method {}.{} returned: {}", controller.getClass().getName(), method.getName(), result);
                handleControllerResult(result, context);
                return result;
            } catch (InvocationTargetException e) {
                // Unwrap the actual exception thrown by the method
                throw e.getCause();
            }
        } catch (Throwable e) {
            logger.error("Failed to invoke controller method: {}.{} - {}", controller.getClass().getName(), method.getName(), e.getMessage());
            throw new RuntimeException("Failed to invoke controller method: " + method.getName(), e);
        }
    }

    /**
     * Handles the result of a controller method invocation and sends the appropriate HTTP response.
     *
     * @param result  The result of the controller method invocation.
     * @param context The HTTP request context used to send the response.
     */
    private void handleControllerResult(Object result, Context context) {
        Response<?> response = convertToResponse(result);
        logger.debug("Handling response with status code {} and body: {}", response.getStatusCode(), response.getBody());
        context.response().handleResponse(response);
    }

    /**
     * Converts any result into a Response object with the appropriate status code and body.
     *
     * @param result The result to convert
     * @return A Response object
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
     * Resolves a method parameter based on its annotations and type.
     * Supports resolving path parameters, query parameters, and the `Context` object.
     *
     * @param parameter The method parameter to resolve.
     * @param context   The HTTP request context, which provides access to path and query parameters.
     * @return The resolved parameter value, or `null` if the parameter type is unsupported.
     */
    private Object resolveParameter(Parameter parameter, Context context) {
        logger.debug("Resolving parameter {} of type {}", parameter.getName(), parameter.getType().getName());

        // Handle path parameters
        PathParameter pathParam = parameter.getAnnotation(PathParameter.class);
        if (pathParam != null) {
            logger.debug("Resolving path parameter {} for parameter {}", pathParam.value(), parameter.getName());
            return resolvePathParameter(pathParam, parameter, context.pathParams().pathParams());
        }

        // Handle query parameters
        QueryParameter queryParam = parameter.getAnnotation(QueryParameter.class);
        if (queryParam != null) {
            logger.debug("Resolving query parameter {} for parameter {}", queryParam.value(), parameter.getName());
            return resolveQueryParameter(queryParam, parameter, context.queryParams().queryParams());
        }

        // Handle request body
        if (parameter.isAnnotationPresent(RequestBody.class)) {
            logger.debug("Resolving request body for parameter {}", parameter.getName());
            Class<?> type = parameter.getType();
            return context.body().parseBodyAs(type);
        }

        // Handle Context parameter
        if (parameter.getType().isAssignableFrom(Context.class)) {
            return context;
        }

        logger.debug("Parameter {} of type {} is not supported", parameter.getName(), parameter.getType().getName());

        // Default to null for unsupported parameters
        return null;
    }

    /**
     * Resolves a path parameter from the request context.
     *
     * @param annotation The `@PathParameter` annotation on the parameter.
     * @param parameter  The method parameter to resolve.
     * @param pathParams A map of path parameter names to their values from the request.
     * @return The resolved path parameter value, converted to the parameter's type.
     * @throws IllegalArgumentException If the required path parameter is missing.
     */
    private Object resolvePathParameter(PathParameter annotation, Parameter parameter, Map<String, String> pathParams) {
        String paramName = annotation.value();
        String paramValue = pathParams.get(paramName);

        if (paramValue == null) {
            logger.debug("Optional parameter not provided, returning null.");
            return null;
        }

        return TypeConverter.convert(paramValue, parameter.getType());
    }

    /**
     * Resolves a query parameter from the request context.
     *
     * @param annotation  The `@QueryParameter` annotation on the parameter.
     * @param parameter   The method parameter to resolve.
     * @param queryParams A map of query parameter names to their values from the request.
     * @return The resolved query parameter value, converted to the parameter's type.
     * @throws IllegalArgumentException If the required query parameter is missing.
     */
    private Object resolveQueryParameter(QueryParameter annotation, Parameter parameter, Map<String, String> queryParams) {
        String paramName = annotation.value();
        String paramValue = queryParams.get(paramName);
        if (paramValue == null) {
            logger.debug("Required query parameter {} not provided, returning null.", paramName);
            throw new IllegalArgumentException("Missing query parameter: " + paramName);
        }
        return TypeConverter.convert(paramValue, parameter.getType());
    }

    /**
     * Finds all classes annotated with `@Service` in the classpath.
     *
     * @return A list of service classes.
     * @throws ClassNotFoundException if a class cannot be loaded.
     * @throws IOException            if an error occurs while reading resources.
     */
    private List<Class<?>> findServices() throws ClassNotFoundException, IOException {
        logger.debug("Finding all service classes in the base package: {}", basePackage);
        List<Class<?>> serviceClasses = new ArrayList<>();
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        String path = basePackage.replace('.', '/');
        var resources = classLoader.getResources(path);

        while (resources.hasMoreElements()) {
            var resource = resources.nextElement();
            logger.debug("Found resource: {}", resource);
            var file = new File(resource.getFile());
            if (file.isDirectory()) {
                logger.debug("Scanning directory: {}", file.getAbsolutePath());
                serviceClasses.addAll(findClassesInDirectory(file, basePackage, Service.class));
            } else if (resource.getProtocol().equals("jar")) {
                logger.debug("Resource is a JAR file: {}", resource.getPath());
                var jarFilePath = resource.getPath().substring(5, resource.getPath().indexOf("!"));
                try (var jarFile = new java.util.jar.JarFile(jarFilePath)) {
                    var entries = jarFile.entries();
                    while (entries.hasMoreElements()) {
                        var entry = entries.nextElement();
                        if (entry.getName().startsWith(path) && entry.getName().endsWith(".class")) {
                            var className = entry.getName().replace('/', '.').substring(0, entry.getName().length() - 6);
                            try {
                                var clazz = Class.forName(className, false, classLoader);
                                if (clazz.isAnnotationPresent(Service.class)) {
                                    logger.debug("Found service class: {}", clazz.getName());
                                    serviceClasses.add(clazz);
                                }
                            } catch (NoClassDefFoundError | UnsupportedClassVersionError ignored) {
                                logger.error("Failed to load class: {}", className);
                            }
                        }
                    }
                }
            }
        }

        logger.info("Completed finding @Service classes in base package. Total found: {}", serviceClasses.size());
        return serviceClasses;
    }

    /**
     * Finds all classes annotated with `@Controller` in the classpath.
     *
     * @return A list of controller classes.
     * @throws ClassNotFoundException if a class cannot be loaded.
     * @throws IOException            if an error occurs while reading resources.
     */
    private List<Class<?>> findControllers() throws ClassNotFoundException, IOException {
        logger.debug("Finding all controller classes in the base package: {}", basePackage);
        List<Class<?>> controllerClasses = new ArrayList<>();
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        String path = basePackage.replace('.', '/');
        var resources = classLoader.getResources(path);

        while (resources.hasMoreElements()) {
            var resource = resources.nextElement();
            System.out.println("Resource: " + resource);
            logger.debug("Found resource: {}", resource);
            var file = new File(resource.getFile());
            if (file.isDirectory()) {
                logger.debug("Scanning directory: {}", file.getAbsolutePath());
                controllerClasses.addAll(findClassesInDirectory(file, basePackage, Controller.class));
            } else if (resource.getProtocol().equals("jar")) {
                logger.debug("Resource is a JAR file: {}", resource.getPath());
                var jarFilePath = resource.getPath().substring(5, resource.getPath().indexOf("!"));
                try (var jarFile = new java.util.jar.JarFile(jarFilePath)) {
                    var entries = jarFile.entries();
                    while (entries.hasMoreElements()) {
                        var entry = entries.nextElement();
                        if (entry.getName().startsWith(path) && entry.getName().endsWith(".class")) {
                            var className = entry.getName().replace('/', '.').substring(0, entry.getName().length() - 6);
                            try {
                                var clazz = Class.forName(className, false, classLoader);
                                if (clazz.isAnnotationPresent(Controller.class)) {
                                    logger.debug("Found controller class: {}", clazz.getName());
                                    controllerClasses.add(clazz);
                                }
                            } catch (NoClassDefFoundError | UnsupportedClassVersionError ignored) {
                                logger.error("Failed to load class: {}", className);
                            }
                        }
                    }
                }
            }
        }

        logger.info("Completed finding @Controller classes in base package. Total found: {}", controllerClasses.size());
        return controllerClasses;
    }

    /**
     * Finds all classes annotated with `@GlobalHandler` in the classpath.
     *
     * @return A list of global handler classes.
     * @throws ClassNotFoundException if a class cannot be loaded.
     * @throws IOException            if an error occurs while reading resources.
     */
    private List<Class<?>> findHandlers() throws ClassNotFoundException, IOException {
        logger.info("Finding all global handler classes in the base package: {}", basePackage);
        List<Class<?>> handlerClasses = new ArrayList<>();
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        String path = basePackage.replace('.', '/');
        var resources = classLoader.getResources(path);

        while (resources.hasMoreElements()) {
            var resource = resources.nextElement();
            var file = new File(resource.getFile());
            if (file.isDirectory()) {
                handlerClasses.addAll(findClassesInDirectory(file, basePackage, GlobalHandler.class));
            } else if (resource.getProtocol().equals("jar")) {
                logger.info("Resource is a JAR file: {}", resource.getPath());
                var jarFilePath = resource.getPath().substring(5, resource.getPath().indexOf("!"));
                try (var jarFile = new java.util.jar.JarFile(jarFilePath)) {
                    var entries = jarFile.entries();
                    while (entries.hasMoreElements()) {
                        var entry = entries.nextElement();
                        if (entry.getName().startsWith(path) && entry.getName().endsWith(".class")) {
                            var className = entry.getName().replace('/', '.').substring(0, entry.getName().length() - 6);
                            try {
                                var clazz = Class.forName(className, false, classLoader);
                                if (clazz.isAnnotationPresent(GlobalHandler.class)) {
                                    logger.info("Found global handler class: {}", clazz.getName());
                                    handlerClasses.add(clazz);
                                }
                            } catch (NoClassDefFoundError | UnsupportedClassVersionError ignored) {
                                logger.info("Failed to load class: {}", className);
                            }
                        }
                    }
                }
            }
        }

        return handlerClasses;
    }


    /**
     * Recursively finds all classes in a directory and its subdirectories.
     * Only classes annotated with `@Service` are included.
     *
     * @param directory   The directory to search.
     * @param packageName The package name corresponding to the directory.
     * @return A list of service classes.
     * @throws ClassNotFoundException if a class cannot be loaded.
     */
    private <A extends Annotation> List<Class<?>> findClassesInDirectory(
            File directory,
            String packageName,
            Class<A> annotationType) throws ClassNotFoundException {

        logger.debug("Finding classes in directory: {} with package name: {}", directory.getAbsolutePath(), packageName);
        List<Class<?>> classes = new ArrayList<>();
        if (!directory.exists()) {
            return classes;
        }

        var files = directory.listFiles();
        if (files != null) {
            for (var file : files) {
                if (file.isDirectory()) {
                    logger.debug("Found subdirectory: {}", file.getAbsolutePath());
                    classes.addAll(findClassesInDirectory(
                            file,
                            packageName + (packageName.isEmpty() ? "" : ".") + file.getName(),
                            annotationType));
                } else if (file.getName().endsWith(".class")) {
                    var className = packageName + (packageName.isEmpty() ? "" : ".") + file.getName().substring(0, file.getName().length() - 6);
                    try {
                        var clazz = Class.forName(className, false, Thread.currentThread().getContextClassLoader());
                        if (clazz.isAnnotationPresent(annotationType) && !clazz.isLocalClass() && !clazz.isAnonymousClass()) {
                            logger.debug("Found class with annotation {}: {}", annotationType.getSimpleName(), clazz.getName());
                            classes.add(clazz);
                        }
                    } catch (NoClassDefFoundError | UnsupportedClassVersionError ignored) {
                        logger.error("Failed to load class: {}", className);
                    }
                }
            }
        }
        return classes;
    }


    /**
     * Checks if a class is registered in the DI container.
     *
     * @param cls The class to check.
     * @return `true` if the class is registered, `false` otherwise.
     */
    public boolean isRegistered(Class<?> cls) {
        return instances.containsKey(cls);
    }

}