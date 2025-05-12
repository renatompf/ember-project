package io.github.renatompf.ember.core.di;

import io.github.renatompf.ember.annotations.controller.Controller;
import io.github.renatompf.ember.annotations.exceptions.GlobalHandler;
import io.github.renatompf.ember.annotations.service.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.reflect.Modifier.isFinal;
import static java.lang.reflect.Modifier.isStatic;

/**
 * The `ComponentRegistry` class is responsible for managing the lifecycle of components
 * in the application. It provides methods to register, resolve, and retrieve components
 * annotated with specific annotations such as `@Service`, `@Controller`, or `@GlobalHandler`.
 */
public class ComponentRegistry {
    private static final Logger logger = LoggerFactory.getLogger(ComponentRegistry.class);
    private final Map<Class<?>, Object> instances = new ConcurrentHashMap<>();
    private final ThreadLocal<Set<Class<?>>> resolving = ThreadLocal.withInitial(HashSet::new);
    private static final Object UNRESOLVED = new Object();

    /**
     * Registers a service class in the registry if it is annotated with `@Service`,
     * `@Controller`, or `@GlobalHandler`.
     *
     * @param serviceClass The class to register.
     * @param <T>          The type of the service class.
     * @throws IllegalArgumentException If the class is not properly annotated.
     */
    public <T> void register(Class<T> serviceClass) {
        if (serviceClass.isAnnotationPresent(Service.class) ||
                serviceClass.isAnnotationPresent(Controller.class) ||
                serviceClass.isAnnotationPresent(GlobalHandler.class)) {
            instances.put(serviceClass, UNRESOLVED);
            logger.info("Registered service: {}", serviceClass.getName());
        } else {
            logger.error("Class {} is not annotated with @Service, @Controller or @GlobalHandler", serviceClass.getName());
            throw new IllegalArgumentException("Class " + serviceClass.getName() + " is not properly annotated");
        }
    }

    /**
     * Registers a list of service classes in the registry.
     *
     * @param classes The list of classes to register.
     */
    public void registerAll(List<Class<?>> classes) {
        for (Class<?> cls : classes) {
            register(cls);
        }
    }

    /**
     * Checks if a class is registered in the registry.
     *
     * @param cls The class to check.
     * @return `true` if the class is registered, `false` otherwise.
     */
    public boolean isRegistered(Class<?> cls) {
        return instances.containsKey(cls);
    }

    /**
     * Resolves all registered services by creating their instances.
     */
    public void resolveAll() {
        for (Class<?> serviceClass : instances.keySet()) {
            resolve(serviceClass);
        }
    }


    /**
     * Resolves a specific service class by creating its instance and injecting dependencies.
     *
     * @param serviceClass The class to resolve.
     * @param <T>          The type of the service class.
     * @return The resolved instance of the service class.
     * @throws IllegalStateException If the service is not registered.
     * @throws RuntimeException      If the service cannot be resolved.
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
                    return createInstance(cls);
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
     * Creates an instance of the given class and injects its dependencies.
     *
     * @param cls The class to instantiate.
     * @param <T> The type of the class.
     * @return The created instance of the class.
     * @throws Exception If the instance cannot be created.
     */
    private <T> Object createInstance(Class<T> cls) throws Exception {
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
                instances.put(cls, newInstance);
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
            instances.put(cls, newInstance);
            logger.info("Successfully resolved service with no dependencies: {}", cls.getName());
            return newInstance;
        }

        // Handle constructors with dependencies
        T newInstance = (T) constructor.newInstance(new Object[paramTypes.length]);
        instances.put(cls, newInstance);

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
    }

    /**
     * Retrieves all resolved instances in the registry.
     *
     * @return A collection of all resolved instances.
     */
    public Collection<Object> getAllInstances() {
        return Collections.unmodifiableCollection(instances.values());
    }

    /**
     * Retrieves the instance of a specific class from the registry.
     *
     * @param cls The class whose instance is to be retrieved.
     * @return The instance of the class, or `null` if not resolved.
     */
    public Object getInstance(Class<?> cls) {
        Object instance = instances.get(cls);
        return instance == UNRESOLVED ? null : instance;
    }

}