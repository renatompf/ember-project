package io.github.renatompf.ember.core.server;

/**
 * Middleware that sets the {@link Context} for the current thread using {@link ContextHolder}.
 * Ensures that the context is cleared after the request is processed.
 */
public class ContextMiddleware implements Middleware {

    /**
     * Default constructor for the ContextMiddleware class.
     */
    public ContextMiddleware() {}

    /**
     * Handles the middleware logic by setting the context, proceeding to the next middleware,
     * and ensuring the context is cleared after processing.
     *
     * @param ctx The {@link Context} of the current request.
     * @throws Exception If an error occurs during middleware execution.
     */
    @Override
    public void handle(Context ctx) throws Exception {
        try {
            ContextHolder.setContext(ctx); // Set the context for the current thread
            ctx.next(); // Proceed to the next middleware or handler
        } finally {
            ContextHolder.clearContext(); // Clear the context after the request
        }
    }
}