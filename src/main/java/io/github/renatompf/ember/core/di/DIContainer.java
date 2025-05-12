package io.github.renatompf.ember.core.di;

import io.github.renatompf.ember.EmberApplication;
import io.github.renatompf.ember.annotations.controller.Controller;
import io.github.renatompf.ember.annotations.exceptions.GlobalHandler;
import io.github.renatompf.ember.annotations.service.Service;
import io.github.renatompf.ember.core.controller.ControllerMapper;
import io.github.renatompf.ember.core.exception.ExceptionHandlerRegistry;
import io.github.renatompf.ember.core.exception.ExceptionManager;
import io.github.renatompf.ember.core.parameter.ParameterResolver;
import io.github.renatompf.ember.core.validation.ValidationManager;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The DIContainer coordinates component discovery, registration, initialization,
 * and wiring of the Ember application's components.
 * <p>
 * It is responsible for:
 * <ul>
 *   <li>Scanning the classpath for annotated components (@Service, @Controller, @GlobalHandler)</li>
 *   <li>Registering and resolving component dependencies</li>
 *   <li>Initializing core framework services (validation, exception handling, parameter resolution)</li>
 *   <li>Mapping controller routes to the Ember application</li>
 * </ul>
 */
public class DIContainer {
    private static final Logger logger = LoggerFactory.getLogger(DIContainer.class);
    private final String basePackage;
    private final ComponentRegistry registry;
    private final ClassScanner classScanner;
    private ExceptionManager exceptionManager;
    private ExceptionHandlerRegistry exceptionHandlerRegistry;
    private ControllerMapper controllerMapper;
    private ValidationManager validationManager;
    private ParameterResolver parameterResolver;

    /**
     * Creates a DIContainer with the default base package.
     */
    public DIContainer() {
        this("");
    }

    /**
     * Creates a DIContainer with the specified base package.
     *
     * @param basePackage The base package to scan for annotated classes
     */
    public DIContainer(String basePackage) {
        this.basePackage = basePackage;
        this.registry = new ComponentRegistry();
        this.classScanner = new ClassScanner();
    }

    /**
     * Creates a DIContainer with the specified base package, component registry, and class scanner.
     *
     * @param basePackage The base package to scan for annotated classes
     * @param registry The component registry to use
     * @param scanner The class scanner to use
     */
    public DIContainer(String basePackage, ComponentRegistry registry, ClassScanner scanner) {
        this.basePackage = basePackage;
        this.registry = registry;
        this.classScanner = scanner;
    }

    /**
     * Initializes the container by discovering, registering, and resolving all components.
     * Also initializes core framework services and prepares controller mapping.
     *
     * @throws RuntimeException if initialization fails
     */
    public void init() {
        try {
            registerServices();
            registerControllers();
            registerGlobalHandlers();

            // Create core components before resolving others
            this.validationManager = new ValidationManager();
            this.parameterResolver = new ParameterResolver();
            this.exceptionManager = new ExceptionManager();

            // Resolve all components to complete initialization
            registry.resolveAll();

            // Initialize components that require resolved dependencies
            initializeExceptionHandling();
            initializeControllerMapper();

            logger.info("Container initialization completed");
        } catch (Exception e) {
            logger.error("Failed to initialize container", e);
            throw new RuntimeException("Container initialization failed", e);
        }
    }


    /**
     * Maps controller routes to the Ember application.
     *
     * @param app The Ember application
     * @throws IllegalStateException if the container is not initialized
     */
    public void mapControllerRoutes(EmberApplication app) {
        if (controllerMapper == null) {
            throw new IllegalStateException("Container not initialized. Call initialize() first.");
        }

        // Convert collection to map for controller mapper
        Map<Class<?>, Object> instances = new HashMap<>();
        for (Object instance : registry.getAllInstances()) {
            instances.put(instance.getClass(), instance);
        }

        controllerMapper.mapControllerRoutes(app, instances);
    }

    /**
     * Registers an individual component class.
     *
     * @param componentClass The component class to register
     * @param <T> The type of the component
     */
    public <T> void register(Class<T> componentClass) {
        registry.register(componentClass);
    }

    /**
     * Resolves all registered components.
     */
    public void resolveAll() {
        registry.resolveAll();
    }

    /**
     * Gets a fully resolved instance of the requested component.
     *
     * @param componentClass The class of the component to resolve
     * @param <T> The type of the component
     * @return The resolved component instance
     */
    public <T> T resolve(Class<T> componentClass) {
        return registry.resolve(componentClass);
    }

    /**
     * Registers all service classes found in the classpath.
     *
     * @throws IOException if classpath scanning fails
     * @throws ClassNotFoundException if a class cannot be loaded
     */
    private void registerServices() throws IOException, ClassNotFoundException {
        logger.info("Registering service classes from package: {}", basePackage);
        List<Class<?>> serviceClasses = classScanner.findAnnotatedClasses(basePackage, Service.class);
        registry.registerAll(serviceClasses);
        logger.info("Registered {} service classes", serviceClasses.size());
    }

    /**
     * Registers all controller classes found in the classpath.
     *
     * @throws IOException if classpath scanning fails
     * @throws ClassNotFoundException if a class cannot be loaded
     */
    private void registerControllers() throws IOException, ClassNotFoundException {
        logger.info("Registering controller classes from package: {}", basePackage);
        List<Class<?>> controllerClasses = classScanner.findAnnotatedClasses(basePackage, Controller.class);
        registry.registerAll(controllerClasses);
        logger.info("Registered {} controller classes", controllerClasses.size());
    }

    /**
     * Registers all global exception handler classes found in the classpath.
     *
     * @throws IOException if classpath scanning fails
     * @throws ClassNotFoundException if a class cannot be loaded
     */
    private void registerGlobalHandlers() throws IOException, ClassNotFoundException {
        logger.info("Registering global handler classes from package: {}", basePackage);
        List<Class<?>> handlerClasses = classScanner.findAnnotatedClasses(basePackage, GlobalHandler.class);
        registry.registerAll(handlerClasses);
        logger.info("Registered {} global handler classes", handlerClasses.size());
    }

    /**
     * Initializes the exception manager with the global exception handler, if present.
     * If no global handler is found, initializes a default handler registry.
     */
    private void initializeExceptionHandling() {
        for (Object instance : registry.getAllInstances()) {
            if (instance != null && instance.getClass().isAnnotationPresent(GlobalHandler.class)) {
                this.exceptionHandlerRegistry = new ExceptionHandlerRegistry(instance);

                // Register handlers from registry to manager using the corrected method name
                exceptionHandlerRegistry.registerHandlersWithManager(exceptionManager);

                logger.info("Initialized exception handling with handler: {}", instance.getClass().getName());
                return;
            }
        }

        // Create a default exception handler registry if no global handler is found
        this.exceptionHandlerRegistry = new ExceptionHandlerRegistry(null);
        logger.info("Initialized default exception handling (no global handler found)");
    }

    /**
     * Initializes the controller mapper with the required dependencies.
     */
    private void initializeControllerMapper() {
        Validator validator = validationManager.getValidator();
        this.controllerMapper = new ControllerMapper(
                exceptionHandlerRegistry,
                parameterResolver
        );
        logger.info("Initialized controller mapper");
    }

    /**
     * Gets the validation manager.
     *
     * @return The validation manager
     */
    public ValidationManager getValidationManager() {
        return validationManager;
    }

    /**
     * Gets the exception manager.
     *
     * @return The exception manager
     */
    public ExceptionManager getExceptionManager() {
        return exceptionManager;
    }

    /**
     * Gets the parameter resolver.
     *
     * @return The parameter resolver
     */
    public ParameterResolver getParameterResolver() {
        return parameterResolver;
    }

    /**
     * Used only for testing to inject a mock ControllerMapper.
     */
    public void setControllerMapper(ControllerMapper controllerMapper) {
        this.controllerMapper = controllerMapper;
    }

}