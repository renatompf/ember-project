package io.ember.core;

import io.ember.EmberApplication;
import io.ember.annotations.controller.Controller;
import io.ember.annotations.http.*;
import io.ember.annotations.middleware.WithMiddleware;
import io.ember.annotations.parameters.PathParameter;
import io.ember.annotations.parameters.QueryParameter;
import io.ember.annotations.parameters.RequestBody;
import io.ember.annotations.service.Service;
import io.ember.exceptions.CircularDependencyException;
import io.ember.utils.TypeConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The `DIContainer` class is a simple implementation of a Dependency Injection (DI) container.
 * The container supports automatic resolution of dependencies and prevents circular dependencies.
 */
public class DIContainer {

    private static final Logger logger = LoggerFactory.getLogger(DIContainer.class);

    // Map to store registered service classes and their instances
    private final Map<Class<?>, Object> instances = new ConcurrentHashMap<>();

    // ThreadLocal to keep track of resolving classes to prevent circular dependencies
    private final ThreadLocal<Set<Class<?>>> resolving = ThreadLocal.withInitial(HashSet::new);

    // Marker object to indicate that a service is registered but not yet resolved
    private static final Object UNRESOLVED = new Object();

    /**
     * Registers a service class in the container.
     * The class must be annotated with `@Service`.
     *
     * @param serviceClass The service class to register.
     * @param <T>          The type of the service class.
     * @throws IllegalArgumentException if the class is not annotated with `@Service`.
     */
    public <T> void register(Class<T> serviceClass) {
        if (serviceClass.isAnnotationPresent(Service.class) || serviceClass.isAnnotationPresent(Controller.class)) {
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
        Object instance = instances.get(serviceClass);
        if (instance != null && instance != UNRESOLVED) {
            logger.debug("Resolved service: {}", serviceClass.getName());
            return (T) instance;
        }

        // Prevent circular dependencies
        if (resolving.get().contains(serviceClass)) {
            logger.error("Circular dependency detected for service: {}", serviceClass.getName());
            throw new CircularDependencyException("Circular dependency detected for: " + serviceClass.getName());
        }

        resolving.get().add(serviceClass);
        try {
            logger.debug("Resolving service: {}", serviceClass.getName());
            // Use computeIfAbsent to ensure only one thread creates the instance
            return (T) instances.computeIfPresent(serviceClass, (cls, existing) -> {
                if (existing != UNRESOLVED) {
                    return existing;
                }

                try {

                    // Check if the class has declared fields
                    if (cls.getDeclaredFields().length == 0) {
                        // No fields, instantiate using the default constructor
                        logger.info("No fields found in {}, using default constructor.", cls.getName());
                        return cls.getDeclaredConstructor().newInstance();
                    }

                    Constructor<?>[] constructors = cls.getConstructors();
                    if (constructors.length != 1) {
                        logger.error("Service must have exactly one public constructor: {}", cls.getName());
                        throw new IllegalStateException("Service must have exactly one public constructor: " + cls.getName());
                    }

                    Constructor<?> constructor = constructors[0];
                    Object[] parameters = Arrays.stream(constructor.getParameterTypes())
                            .map(this::resolve)
                            .toArray();

                    logger.info("Successfully resolved service: {}", cls.getName());
                    return constructor.newInstance(parameters);
                } catch (CircularDependencyException e) {
                    throw e;
                } catch (IllegalStateException e){
                    logger.error("Service has circular dependencies: {}", cls.getName());
                    throw e;
                } catch (Exception e) {
                    logger.error("Failed to resolve service: {}", cls.getName(), e);
                    throw new RuntimeException("Failed to resolve service: " + cls.getName(), e);
                }
            });
        } finally {
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
            logger.error("Error while handling middleware or invoking controller method: {}", e.getMessage());
            context.response().internalServerError("Error: " + e.getMessage());
        }
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

            Object result = method.invoke(controller, args);
            logger.debug("Controller method {}.{} returned: {}", controller.getClass().getName(), method.getName(), result);
            handleControllerResult(result, context);
            return result;
        } catch (Exception e) {
            logger.error("Failed to invoke controller method: {}.{} - {}", controller.getClass().getName(), method.getName(), e.getMessage());
            throw new RuntimeException("Failed to invoke controller method: " + method.getName(), e);
        }
    }

    /**
     * Handles the result of a controller method invocation and sends the appropriate HTTP response.
     * <p>
     * If the result is an instance of `Response`, it sends the response body and status code.
     * If the result is not null, it sends the result's string representation with a 200 status code.
     * If the result is null, it sends an empty response with a 204 status code.
     *
     * @param result  The result of the controller method invocation.
     * @param context The HTTP request context used to send the response.
     */
    private void handleControllerResult(Object result, Context context) {
        if (result instanceof Response response) {
            logger.debug("Sending response with status code {} and body: {}", response.statusCode(), response.body());
            context.response().sendJson(response.body(), response.statusCode());
        } else if (result != null) {
            logger.debug("Sending response with status code 200 and body: {}", result);
            context.response().sendJson(result.toString(), 200);
        } else {
            logger.debug("Sending empty response with status code 204");
            context.response().sendJson("", 204);
        }
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
    private static List<Class<?>> findServices() throws ClassNotFoundException, IOException {
        logger.info("Finding all service classes in the classpath");
        List<Class<?>> serviceClasses = new ArrayList<>();
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        var resources = classLoader.getResources(""); // Empty string gets all resources

        while (resources.hasMoreElements()) {
            var resource = resources.nextElement();
            logger.debug("Found resource: {}", resource);
            var file = new File(resource.getFile());
            if (file.isDirectory()) {
                logger.debug("Scanning directory: {}", file.getAbsolutePath());
                serviceClasses.addAll(findClassesInDirectory(file, "", Service.class));
            } else if (resource.getProtocol().equals("jar")) {
                var jarFilePath = resource.getPath().substring(5, resource.getPath().indexOf("!"));
                logger.debug("Resource is a JAR file: {}", jarFilePath);
                try (var jarFile = new java.util.jar.JarFile(jarFilePath)) {
                    var entries = jarFile.entries();
                    while (entries.hasMoreElements()) {
                        var entry = entries.nextElement();
                        if (entry.getName().endsWith(".class")) {
                            var className = entry.getName().replace('/', '.').substring(0, entry.getName().length() - 6);
                            try {
                                var clazz = Class.forName(className, false, classLoader); // Don't initialize yet
                                if (clazz.isAnnotationPresent(Service.class)) {
                                    logger.debug("Found service class: {}", clazz.getName());
                                    serviceClasses.add(clazz);
                                }
                            } catch (NoClassDefFoundError | UnsupportedClassVersionError ignored) {
                                // Log or handle errors as needed
                                logger.error("Failed to load class: {}", className);
                            }
                        }
                    }
                }
            }
        }

        logger.info("Completed finding @Service classes. Total found: {}", serviceClasses.size());
        return serviceClasses;
    }

    /**
     * Finds all classes annotated with `@Controller` in the classpath.
     *
     * @return A list of controller classes.
     * @throws ClassNotFoundException if a class cannot be loaded.
     * @throws IOException            if an error occurs while reading resources.
     */
    private static List<Class<?>> findControllers() throws ClassNotFoundException, IOException {
        logger.info("Finding all controller classes in the classpath");
        List<Class<?>> controllerClasses = new ArrayList<>();
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        var resources = classLoader.getResources(""); // Empty string gets all resources

        while (resources.hasMoreElements()) {
            var resource = resources.nextElement();
            logger.debug("Found resource: {}", resource);
            var file = new File(resource.getFile());
            if (file.isDirectory()) {
                logger.debug("Scanning directory: {}", file.getAbsolutePath());
                controllerClasses.addAll(findClassesInDirectory(file, "", Controller.class));
            } else if (resource.getProtocol().equals("jar")) {
                logger.debug("Resource is a JAR file: {}", resource.getPath());
                var jarFilePath = resource.getPath().substring(5, resource.getPath().indexOf("!"));
                try (var jarFile = new java.util.jar.JarFile(jarFilePath)) {
                    var entries = jarFile.entries();
                    while (entries.hasMoreElements()) {
                        var entry = entries.nextElement();
                        if (entry.getName().endsWith(".class")) {
                            var className = entry.getName().replace('/', '.').substring(0, entry.getName().length() - 6);
                            try {
                                var clazz = Class.forName(className, false, classLoader); // Don't initialize yet
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
        logger.info("Completed finding @Controller classes. Total found: {}", controllerClasses.size());
        return controllerClasses;
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
    private static <A extends Annotation> List<Class<?>> findClassesInDirectory(
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