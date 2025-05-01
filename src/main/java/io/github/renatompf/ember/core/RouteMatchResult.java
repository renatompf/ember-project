package io.github.renatompf.ember.core;

import java.util.Map;

/**
 * Represents the result of a route match, containing the middleware chain
 * to be executed and the extracted path parameters.
 *
 * @param middlewareChain The middleware chain associated with the matched route.
 * @param parameters      The extracted path parameters from the route.
 */
public record RouteMatchResult(MiddlewareChain middlewareChain, Map<String, String> parameters) {
}
