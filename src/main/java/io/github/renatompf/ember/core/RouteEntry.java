package io.github.renatompf.ember.core;

import io.github.renatompf.ember.enums.HttpMethod;

import java.util.Objects;

/**
 * Represents a single route entry in the router, containing the HTTP method,
 * the route pattern, and the middleware chain to handle requests.
 */
public class RouteEntry {

    /** The HTTP method associated with this route (e.g., GET, POST). */
    private final HttpMethod method;

    /** The pattern used to match the route's path. */
    private final RoutePattern pattern;

    /** The middleware chain to handle requests for this route. */
    private final MiddlewareChain middlewareChain;

    /**
     * Constructs a new `RouteEntry` with the specified HTTP method, path, and middleware chain.
     *
     * @param method          The HTTP method for the route.
     * @param path            The path pattern for the route.
     * @param middlewareChain The middleware chain to handle requests for this route.
     */
    public RouteEntry(HttpMethod method, String path, MiddlewareChain middlewareChain) {
        this.method = method;
        this.pattern = new RoutePattern(path);
        this.middlewareChain = middlewareChain;
    }

    /**
     * Gets the HTTP method associated with this route.
     *
     * @return The HTTP method.
     */
    public HttpMethod getMethod() {
        return method;
    }

    /**
     * Gets the route pattern used to match the path.
     *
     * @return The route pattern.
     */
    public RoutePattern getPattern() {
        return pattern;
    }

    /**
     * Gets the middleware chain to handle requests for this route.
     *
     * @return The middleware chain.
     */
    public MiddlewareChain getMiddlewareChain() {
        return middlewareChain;
    }

    /**
     * Checks if this `RouteEntry` is equal to another object.
     *
     * @param o The object to compare with.
     * @return `true` if the objects are equal, `false` otherwise.
     */
    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        RouteEntry that = (RouteEntry) o;
        return method == that.method && Objects.equals(pattern, that.pattern) && Objects.equals(middlewareChain, that.middlewareChain);
    }

    /**
     * Computes the hash code for this `RouteEntry`.
     *
     * @return The hash code.
     */
    @Override
    public int hashCode() {
        return Objects.hash(method, pattern, middlewareChain);
    }
}