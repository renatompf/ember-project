package io.github.renatompf.ember.core.server;

/**
 * A functional interface representing a middleware component in the application.
 * Middleware is responsible for handling a given `Context` and may throw an exception.
 */
@FunctionalInterface
public interface Middleware {

    /**
     * Handles the given context.
     *
     * @param context The context to handle.
     * @throws Exception If an error occurs while handling the context.
     */
    void handle(Context context) throws Exception;
}
