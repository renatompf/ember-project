package io.ember.core;

import io.ember.EmberApplication;
import io.ember.annotations.controller.Controller;
import io.ember.annotations.http.*;
import io.ember.annotations.service.Service;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;

/**
 * The `DIContainer` class is a simple implementation of a Dependency Injection (DI) container.
 * The container supports automatic resolution of dependencies and prevents circular dependencies.
 */
public class DIContainer {
    // Map to store registered service classes and their instances
    private final Map<Class<?>, Object> instances = new HashMap<>();

    // ThreadLocal to keep track of resolving classes to prevent circular dependencies
    private final ThreadLocal<Set<Class<?>>> resolving = ThreadLocal.withInitial(HashSet::new);

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
            instances.put(serviceClass, null); // Mark as registered
        } else {
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
     *
     * @param serviceClass The service class to resolve.
     * @param <T>          The type of the service class.
     * @return The resolved instance of the service class.
     * @throws IllegalStateException if the service is not registered, has circular dependencies,
     *                               or has more than one public constructor.
     * @throws RuntimeException      if an error occurs during service instantiation.
     */
    public <T> T resolve(Class<T> serviceClass) {
        if (!instances.containsKey(serviceClass)) {
            throw new IllegalStateException("No service registered for: " + serviceClass.getName());
        }

        if (instances.get(serviceClass) != null) {
            return serviceClass.cast(instances.get(serviceClass));
        }

        if (resolving.get().contains(serviceClass)) {
            throw new IllegalStateException("Circular dependency detected for: " + serviceClass.getName());
        }

        resolving.get().add(serviceClass);
        try {
            Constructor<?>[] constructors = serviceClass.getConstructors();
            if (constructors.length != 1) {
                throw new IllegalStateException("Service must have exactly one public constructor: " + serviceClass.getName());
            }

            Constructor<?> constructor = constructors[0];
            Class<?>[] parameterTypes = constructor.getParameterTypes();
            Object[] parameters = new Object[parameterTypes.length];

            for (int i = 0; i < parameterTypes.length; i++) {
                parameters[i] = resolve(parameterTypes[i]);
            }

            T instance = serviceClass.cast(constructor.newInstance(parameters));
            instances.put(serviceClass, instance);
            return instance;
        } catch (Exception e) {
            throw new RuntimeException("Failed to resolve service: " + serviceClass.getName(), e);
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
            System.out.println("No controllers registered to map routes for");
            return;
        }

        for (Object controller : instances.values()) {
            if (controller == null) {
                continue;
            }

            Class<?> clazz = controller.getClass();
            if (clazz.isAnnotationPresent(Controller.class)) {
                String basePath = clazz.getAnnotation(Controller.class).value();

                for (Method method : clazz.getDeclaredMethods()) {
                    if (method.isAnnotationPresent(Get.class)) {
                        String path = combinePaths(basePath, method.getAnnotation(Get.class).value());
                        app.get(path, ctx -> invokeControllerMethod(controller, method, ctx));
                    } else if (method.isAnnotationPresent(Post.class)) {
                        String path = combinePaths(basePath, method.getAnnotation(Post.class).value());
                        app.post(path, ctx -> invokeControllerMethod(controller, method, ctx));
                    } else if (method.isAnnotationPresent(Put.class)) {
                        String path = combinePaths(basePath, method.getAnnotation(Put.class).value());
                        app.put(path, ctx -> invokeControllerMethod(controller, method, ctx));
                    } else if (method.isAnnotationPresent(Delete.class)) {
                        String path = combinePaths(basePath, method.getAnnotation(Delete.class).value());
                        app.delete(path, ctx -> invokeControllerMethod(controller, method, ctx));
                    } else if (method.isAnnotationPresent(Patch.class)) {
                        String path = combinePaths(basePath, method.getAnnotation(Patch.class).value());
                        app.patch(path, ctx -> invokeControllerMethod(controller, method, ctx));
                    } else if (method.isAnnotationPresent(Options.class)) {
                        String path = combinePaths(basePath, method.getAnnotation(Options.class).value());
                        app.options(path, ctx -> invokeControllerMethod(controller, method, ctx));
                    } else if (method.isAnnotationPresent(Head.class)) {
                        String path = combinePaths(basePath, method.getAnnotation(Head.class).value());
                        app.head(path, ctx -> invokeControllerMethod(controller, method, ctx));
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
        if (base.isEmpty()) return path;
        if (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        if (!path.startsWith("/")) path = "/" + path;
        return base + path;
    }

    /**
     * Invokes a controller method with the provided context.
     *
     * @param controller The controller instance.
     * @param method     The method to invoke.
     * @param ctx        The context to pass to the method.
     */
    public Object invokeControllerMethod(Object controller, Method method, Context context) {
        try {
            Class<?>[] paramTypes = method.getParameterTypes();
            if (paramTypes.length == 0) {
                return method.invoke(controller);
            } else {
                return method.invoke(controller, context);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke controller method: " + method.getName(), e);
        }
    }

    /**
     * Finds all classes annotated with `@Service` in the classpath.
     *
     * @return A list of service classes.
     * @throws ClassNotFoundException if a class cannot be loaded.
     * @throws IOException            if an error occurs while reading resources.
     */
    private static List<Class<?>> findServices() throws ClassNotFoundException, IOException {
        List<Class<?>> serviceClasses = new ArrayList<>();
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        var resources = classLoader.getResources(""); // Empty string gets all resources

        while (resources.hasMoreElements()) {
            var resource = resources.nextElement();
            var file = new File(resource.getFile());
            if (file.isDirectory()) {
                serviceClasses.addAll(findClassesInDirectory(file, "", Service.class));
            } else if (resource.getProtocol().equals("jar")) {
                var jarFilePath = resource.getPath().substring(5, resource.getPath().indexOf("!"));
                try (var jarFile = new java.util.jar.JarFile(jarFilePath)) {
                    var entries = jarFile.entries();
                    while (entries.hasMoreElements()) {
                        var entry = entries.nextElement();
                        if (entry.getName().endsWith(".class")) {
                            var className = entry.getName().replace('/', '.').substring(0, entry.getName().length() - 6);
                            try {
                                var clazz = Class.forName(className, false, classLoader); // Don't initialize yet
                                if (clazz.isAnnotationPresent(Service.class)) {
                                    serviceClasses.add(clazz);
                                }
                            } catch (NoClassDefFoundError | UnsupportedClassVersionError ignored) {
                                // Log or handle errors as needed
                            }
                        }
                    }
                }
            }
        }
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
        List<Class<?>> controllerClasses = new ArrayList<>();
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        var resources = classLoader.getResources(""); // Empty string gets all resources

        while (resources.hasMoreElements()) {
            var resource = resources.nextElement();
            var file = new File(resource.getFile());
            if (file.isDirectory()) {
                controllerClasses.addAll(findClassesInDirectory(file, "", Controller.class));
            } else if (resource.getProtocol().equals("jar")) {
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
                                    controllerClasses.add(clazz);
                                }
                            } catch (NoClassDefFoundError | UnsupportedClassVersionError ignored) {
                                // Log or handle errors as needed
                            }
                        }
                    }
                }
            }
        }
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

        List<Class<?>> classes = new ArrayList<>();
        if (!directory.exists()) {
            return classes;
        }

        var files = directory.listFiles();
        if (files != null) {
            for (var file : files) {
                if (file.isDirectory()) {
                    classes.addAll(findClassesInDirectory(
                            file,
                            packageName + (packageName.isEmpty() ? "" : ".") + file.getName(),
                            annotationType));
                } else if (file.getName().endsWith(".class")) {
                    var className = packageName + (packageName.isEmpty() ? "" : ".") + file.getName().substring(0, file.getName().length() - 6);
                    try {
                        var clazz = Class.forName(className, false, Thread.currentThread().getContextClassLoader());
                        if (clazz.isAnnotationPresent(annotationType)) {
                            classes.add(clazz);
                        }
                    } catch (NoClassDefFoundError | UnsupportedClassVersionError ignored) {
                        // Log or handle errors as needed
                    }
                }
            }
        }
        return classes;
    }
}