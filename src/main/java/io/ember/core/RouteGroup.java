package io.ember.core;

import io.ember.EmberApplication;
import io.ember.enums.HttpMethod;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * The `RouteGroup` class allows grouping of routes under a common prefix
 * and applying middleware to all routes within the group. It provides
 * methods to define routes for various HTTP methods and ensures that
 * the grouped middleware is applied to each route.
 */
public class RouteGroup {

    // The common prefix for all routes in this group
    private final String prefix;

    // List of middleware applied to all routes in this group
    private final List<Middleware> groupedMiddleware = new ArrayList<>();

    // Reference to the parent EmberApplication instance
    private final EmberApplication application;

    /**
     * Constructs a new `RouteGroup` with the specified prefix and application.
     *
     * @param prefix      The common prefix for all routes in this group.
     * @param application The parent `EmberApplication` instance.
     */
    public RouteGroup(String prefix, EmberApplication application) {
        this.prefix = prefix
                .endsWith("/") ?
                prefix.substring(0, prefix.length() - 1) :
                prefix;
        this.application = application;
    }

    /**
     * Adds middleware to the group. The middleware will be applied to all
     * routes defined within this group.
     *
     * @param middleware The middleware to add.
     * @return The current `RouteGroup` instance for method chaining.
     */
    public RouteGroup use(Middleware middleware) {
        groupedMiddleware.add(middleware);
        return this;
    }

    /**
     * Creates a new subgroup with a specified prefix and allows the user
     * to define routes and middleware within that subgroup.
     *
     * @param subPrefix      The prefix for the subgroup.
     * @param groupConsumer  A consumer to define routes and middleware in the subgroup.
     * @return The current `RouteGroup` instance for method chaining.
     */
    public RouteGroup group(String subPrefix, Consumer<RouteGroup> groupConsumer) {
        String fullPrefix = getFullPath(subPrefix);

        RouteGroup subGroup = new RouteGroup(fullPrefix, application);
        subGroup.groupedMiddleware.addAll(this.groupedMiddleware);

        groupConsumer.accept(subGroup);

        return this;
    }

    /**
     * Registers a GET route within the group.
     *
     * @param path    The path for the route.
     * @param handler The handler to process requests to this route.
     * @return The current `RouteGroup` instance for method chaining.
     */
    public RouteGroup get(String path, Consumer<Context> handler) {
        application.registerRoute(HttpMethod.GET, prefix + path, handler, groupedMiddleware);
        return this;
    }

    /**
     * Registers a POST route within the group.
     *
     * @param path    The path for the route.
     * @param handler The handler to process requests to this route.
     * @return The current `RouteGroup` instance for method chaining.
     */
    public RouteGroup post(String path, Consumer<Context> handler) {
        application.registerRoute(HttpMethod.POST, prefix + path, handler, groupedMiddleware);
        return this;
    }

    /**
     * Registers a PUT route within the group.
     *
     * @param path    The path for the route.
     * @param handler The handler to process requests to this route.
     * @return The current `RouteGroup` instance for method chaining.
     */
    public RouteGroup put(String path, Consumer<Context> handler) {
        application.registerRoute(HttpMethod.PUT, prefix + path, handler, groupedMiddleware);
        return this;
    }

    /**
     * Registers a DELETE route within the group.
     *
     * @param path    The path for the route.
     * @param handler The handler to process requests to this route.
     * @return The current `RouteGroup` instance for method chaining.
     */
    public RouteGroup delete(String path, Consumer<Context> handler) {
        application.registerRoute(HttpMethod.DELETE, prefix + path, handler, groupedMiddleware);
        return this;
    }

    /**
     * Registers a PATCH route within the group.
     *
     * @param path    The path for the route.
     * @param handler The handler to process requests to this route.
     * @return The current `RouteGroup` instance for method chaining.
     */
    public RouteGroup patch(String path, Consumer<Context> handler) {
        application.registerRoute(HttpMethod.PATCH, prefix + path, handler, groupedMiddleware);
        return this;
    }

    /**
     * Registers an OPTIONS route within the group.
     *
     * @param path    The path for the route.
     * @param handler The handler to process requests to this route.
     * @return The current `RouteGroup` instance for method chaining.
     */
    public RouteGroup options(String path, Consumer<Context> handler) {
        application.registerRoute(HttpMethod.OPTIONS, prefix + path, handler, groupedMiddleware);
        return this;
    }

    /**
     * Registers a HEAD route within the group.
     *
     * @param path    The path for the route.
     * @param handler The handler to process requests to this route.
     * @return The current `RouteGroup` instance for method chaining.
     */
    public RouteGroup head(String path, Consumer<Context> handler) {
        application.registerRoute(HttpMethod.HEAD, prefix + path, handler, groupedMiddleware);
        return this;
    }

    /**
     * Constructs the full path for a route by combining the group's prefix
     * with the specified path.
     *
     * @param path The path to append to the group's prefix.
     * @return The full path for the route.
     */
    private String getFullPath(String path) {
        return prefix + (path.startsWith("/") ? path : "/" + path);
    }
}


