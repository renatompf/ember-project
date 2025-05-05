package io.github.renatompf.ember.core;

import io.github.renatompf.ember.enums.HttpMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger logger = LoggerFactory.getLogger(Router.class);

    private final List<RouteEntry> routes = new ArrayList<>();

    /**
     * Default constructor for the Router class.
     * <p>
     * Initializes an empty list of routes.
     */
    public Router() {}

    /**
     * Registers a route with the specified HTTP method, path, and handler.
     *
     * @param method  The HTTP method for the route (e.g., GET, POST).
     * @param path    The path for the route.
     * @param handler The handler to process requests to this route.
     */
    public void register(HttpMethod method, String path, Consumer<Context> handler) {
        logger.debug("Registering route: method={}, path={}, handler={}", method, path, handler);
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
        logger.debug("Registering route: method={}, path={}, chain={}", method, path, chain);
        routes.add(new RouteEntry(method, path, chain));
    }

    /**
     * Retrieves the route that matches the specified HTTP method and path.
     * <p>
     * This method searches through the registered routes and identifies the best match
     * for the given HTTP method and path. It resolves parameters for dynamic segments
     * if the route contains placeholders (like `:id` or `*`).
     * <p>
     * To ensure correct matching, routes are sorted by specificity:
     * <ul>
     *   <li>Exact matches (e.g., `/example/get`) are prioritized.</li>
     *   <li>Dynamic segments (`:id`) are processed after exact matches.</li>
     *   <li>Optional parameters (`:id?`) and wildcard paths (`*`) are processed last.</li>
     * </ul>
     * If a match is found, it returns a `RouteMatchResult` containing the middleware chain
     * and any extracted parameters. If no match is found, `null` is returned.
     *
     * @param method The HTTP method of the request (e.g., GET, POST, DELETE).
     * @param path   The path of the request (e.g., `/example/get`).
     * @return A `RouteMatchResult` containing the middleware chain and extracted parameters,
     *         or `null` if no matching route is found.
     */
    public RouteMatchResult getRoute(HttpMethod method, String path) {
        logger.debug("Finding route for method: {}, path: {}", method, path);

        // Sort routes by specificity: exact matches first, then dynamic segments
        routes.sort((route1, route2) -> {
            boolean exact1 = isExactMatch(route1.getPattern().getRawPath());
            boolean exact2 = isExactMatch(route2.getPattern().getRawPath());

            if (exact1 && !exact2){
                return -1; // Exact matches come first
            }

            if (!exact1 && exact2) {
                return 1;  // Dynamic matches come after exact matches
            }

            // If both are exact or both are dynamic, sort by path length (longer paths first)
            return Integer.compare(route2.getPattern().getRawPath().length(), route1.getPattern().getRawPath().length());
        });

        // Iterate through the sorted routes to find a match
        for (RouteEntry entry : routes) {
            logger.debug("Checking route: method={}, path={}, pattern={}", method, path, entry.getPattern().getRawPath());
            // Check if the route matches both method and path
            if (entry.getMethod() == method && entry.getPattern().matches(path)) {
                // Extract parameters from the path and return the match result
                Map<String, String> params = entry.getPattern().extractParameters(path);
                logger.debug("Route matched: method={}, path={}, params={}", method, path, params);
                return new RouteMatchResult(entry.getMiddlewareChain(), params);
            }
        }

        // If no match is found, log a warning and return null
        logger.warn("No matching route found: method={}, path={}", method, path);
        return null;
    }

    /**
     * Determines if a route path is an exact match (no dynamic segments).
     * <p>
     * This helper method checks whether the given route path contains any placeholders
     * (`:` for dynamic segments or `*` for wildcards).
     * Exact paths, such as `/example/get`,
     * are prioritized during route matching to avoid conflicts with dynamic routes.
     * </p>
     *
     * @param rawPath The raw path string of the route (e.g., `/example/get` or `/example/:id`).
     * @return `true` if the raw path is an exact match, `false` if it contains dynamic segments.
     */
    private boolean isExactMatch(String rawPath) {
        boolean isExact = !rawPath.contains(":") && !rawPath.contains("*");
        logger.debug("Checking if path is exact: path={}, isExact={}", rawPath, isExact);
        return isExact;
    }


}