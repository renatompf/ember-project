package io.ember.core;

import io.ember.enums.HttpMethod;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * The `Router` class is responsible for managing and matching routes in the application.
 * It allows registering routes with specific HTTP methods and paths, and retrieving
 * the appropriate route for a given HTTP method and path.
 */
public class Router {

    private final List<RouteEntry> routes = new ArrayList<>();

    /**
     * Registers a route with the specified HTTP method, path, and handler.
     *
     * @param method  The HTTP method for the route (e.g., GET, POST).
     * @param path    The path for the route.
     * @param handler The handler to process requests to this route.
     */
    public void register(HttpMethod method, String path, Consumer<Context> handler) {
        routes.add(new RouteEntry(method, path, new MiddlewareChain(List.of(), handler)));
    }

    /**
     * Registers a route with the specified HTTP method, path, and middleware chain.
     *
     * @param method The HTTP method for the route (e.g., GET, POST).
     * @param path   The path for the route.
     * @param chain  The middleware chain to process requests to this route.
     */
    public void register(HttpMethod method, String path, MiddlewareChain chain) {
        routes.add(new RouteEntry(method, path, chain));
    }

    /**
     * Retrieves the route that matches the specified HTTP method and path.
     *
     * @param method The HTTP method of the request.
     * @param path   The path of the request.
     * @return A `RouteMatchResult` containing the middleware chain and extracted parameters,
     *         or `null` if no matching route is found.
     */
    public RouteMatchResult getRoute(HttpMethod method, String path) {
        for (RouteEntry entry : routes) {
            if (entry.getMethod() == method && entry.getPattern().matches(path)) {
                Map<String, String> params = entry.getPattern().extractParameters(path);
                return new RouteMatchResult(entry.getMiddlewareChain(), params);
            }
        }

        return null;
    }

}