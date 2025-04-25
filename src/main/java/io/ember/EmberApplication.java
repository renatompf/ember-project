package io.ember;

import io.ember.core.*;
import io.ember.enums.HttpMethod;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * The `EmberApplication` class provides a fluent API for defining routes, middleware, and starting the server.
 * It acts as the main entry point for building and running the application.
 */
public class EmberApplication {
    // Router instance to manage route registrations and matching
    private final Router router = new Router();

    // Dependency Injection container for managing service instances
    private final DIContainer diContainer = new DIContainer();

    // List of global middleware applied to all routes
    private final List<Middleware> middleware = new ArrayList<>();

    // Server instance to handle HTTP requests
    private final Server server = new Server(router, middleware);

    // Error handler for handling exceptions during request processing
    private BiConsumer<Context, Exception> errorHandler;

    /**
     * Registers a GET route with the specified path and handler.
     *
     * @param path    The path for the route.
     * @param handler The handler to process requests to this route.
     * @return The current `EmberApplication` instance for method chaining.
     */
    public EmberApplication get(String path, Consumer<Context> handler) {
        router.register(HttpMethod.GET, path, handler);
        return this;
    }

    /**
     * Registers a POST route with the specified path and handler.
     *
     * @param path    The path for the route.
     * @param handler The handler to process requests to this route.
     * @return The current `EmberApplication` instance for method chaining.
     */
    public EmberApplication post(String path, Consumer<Context> handler) {
        router.register(HttpMethod.POST, path, handler);
        return this;
    }

    /**
     * Registers a PUT route with the specified path and handler.
     *
     * @param path    The path for the route.
     * @param handler The handler to process requests to this route.
     * @return The current `EmberApplication` instance for method chaining.
     */
    public EmberApplication put(String path, Consumer<Context> handler) {
        router.register(HttpMethod.PUT, path, handler);
        return this;
    }

    /**
     * Registers a DELETE route with the specified path and handler.
     *
     * @param path    The path for the route.
     * @param handler The handler to process requests to this route.
     * @return The current `EmberApplication` instance for method chaining.
     */
    public EmberApplication delete(String path, Consumer<Context> handler) {
        router.register(HttpMethod.DELETE, path, handler);
        return this;
    }

    /**
     * Registers a PATCH route with the specified path and handler.
     *
     * @param path    The path for the route.
     * @param handler The handler to process requests to this route.
     * @return The current `EmberApplication` instance for method chaining.
     */
    public EmberApplication patch(String path, Consumer<Context> handler) {
        router.register(HttpMethod.PATCH, path, handler);
        return this;
    }

    /**
     * Registers an OPTIONS route with the specified path and handler.
     *
     * @param path    The path for the route.
     * @param handler The handler to process requests to this route.
     * @return The current `EmberApplication` instance for method chaining.
     */
    public EmberApplication options(String path, Consumer<Context> handler) {
        router.register(HttpMethod.OPTIONS, path, handler);
        return this;
    }

    /**
     * Registers a HEAD route with the specified path and handler.
     *
     * @param path    The path for the route.
     * @param handler The handler to process requests to this route.
     * @return The current `EmberApplication` instance for method chaining.
     */
    public EmberApplication head(String path, Consumer<Context> handler) {
        router.register(HttpMethod.HEAD, path, handler);
        return this;
    }

    /**
     * Registers a route with a custom middleware chain.
     *
     * @param method     The HTTP method for the route.
     * @param path       The path for the route.
     * @param handler    The handler to process requests to this route.
     * @param middleware The list of middleware to apply to this route.
     */
    public void registerRoute(HttpMethod method, String path, Consumer<Context> handler, List<Middleware> middleware) {
        router.register(method, path, new MiddlewareChain(middleware, handler));
    }

    /**
     * Creates a route group with a common prefix and applies the provided group configuration.
     *
     * @param prefix        The common prefix for all routes in the group.
     * @param groupConsumer A consumer to define the routes and middleware for the group.
     * @return The current `EmberApplication` instance for method chaining.
     */
    public EmberApplication group(String prefix, Consumer<RouteGroup> groupConsumer) {
        RouteGroup routeGroup = new RouteGroup(prefix, this);
        groupConsumer.accept(routeGroup);
        return this;
    }

    /**
     * Adds a global middleware to the application.
     *
     * @param m The middleware to add.
     * @return The current `EmberApplication` instance for method chaining.
     */
    public EmberApplication use(Middleware m) {
        middleware.add(m);
        return this;
    }

    /**
     * Sets a custom error handler for handling exceptions during request processing.
     *
     * @param handler The error handler to set.
     * @return The current `EmberApplication` instance for method chaining.
     */
    public EmberApplication onError(BiConsumer<Context, Exception> handler) {
        this.errorHandler = handler;
        return this;
    }

    /**
     * Retrieves the router instance used by the application.
     *
     * @return The router instance.
     */
    public Router getRouter() {
        return router;
    }


    /**
     * Retrieves the List of Middleware used by the application.
     *
     * @return The list of middleware.
     */
    public List<Middleware> getMiddleware() {
        return middleware;
    }

    /**
     * Retrieves the error handler used by the application.
     *
     * @return the error handler
     */
    public BiConsumer<Context, Exception> getErrorHandler() {
        return errorHandler;
    }

    /**
     * Starts the server on the specified port.
     *
     * @param port The port number to start the server on.
     */
    public void start(int port) {
        // Register all services in the Dependency Injection (DI) container.
        // This step ensures that all service classes are available for injection.
        diContainer.registerServices();

        // Register all controllers in the DI container.
        // Controllers are responsible for handling HTTP requests and defining routes.
        diContainer.registerControllers();

        // Resolve all services to ensure they are instantiated and their dependencies are satisfied.
        // This step validates the DI container's configuration.
        diContainer.resolveAll();

        // Map all routes defined in the controllers to the router.
        // This step binds the routes to their respective handlers in the application.
        diContainer.mapControllerRoutes(this);

        // Start the HTTP server on the specified port.
        // The server will begin listening for incoming requests.
        server.start(port);
    }
}

