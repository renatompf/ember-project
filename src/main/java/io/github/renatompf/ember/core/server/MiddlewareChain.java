package io.github.renatompf.ember.core.server;

import java.util.List;
import java.util.function.Consumer;

/**
 * Represents a chain of middleware and a final handler to process a request.
 *
 * @param middleware A list of middleware to be executed in order.
 * @param handler    The final handler to process the request after middleware execution.
 */
public record MiddlewareChain(List<Middleware> middleware, Consumer<Context> handler) {
}
