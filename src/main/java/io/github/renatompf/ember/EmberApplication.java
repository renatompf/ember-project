package io.github.renatompf.ember;

import io.github.renatompf.ember.core.di.DIContainer;
import io.github.renatompf.ember.core.routing.Router;
import io.github.renatompf.ember.core.server.Context;
import io.github.renatompf.ember.core.server.Middleware;
import io.github.renatompf.ember.core.server.Server;
import io.github.renatompf.ember.enums.HttpMethod;

import java.util.ArrayList;
import java.util.List;
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

    /**
     * Constructs a new `EmberApplication` instance.
     * Initializes the router, DI container, and server.
     */
    public EmberApplication() {}

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
     * Starts the server on the specified port.
     *
     * @param port The port number to start the server on.
     */
    public void start(int port) {
        // Initialize the DI container
        // This step discovers all services, controllers, and handlers and resolves their dependencies
        diContainer.init();

        // Map all routes defined in the controllers to the router.
        // This step binds the routes to their respective handlers in the application.
        diContainer.mapControllerRoutes(this);

        // Start the HTTP server on the specified port.
        // The server will begin listening for incoming requests.
        server.start(port);
    }
}

