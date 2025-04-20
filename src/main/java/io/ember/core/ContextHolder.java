package io.ember.core;

/**
 * A utility class that provides thread-local storage for the {@link Context} object.
 * This ensures that each thread has its own isolated instance of the {@link Context}.
 */
public class ContextHolder {
    /**
     * Thread-local variable to store the {@link Context} for the current thread.
     */
    private static final ThreadLocal<Context> contextThreadLocal = new ThreadLocal<>();

    /**
     * Sets the {@link Context} for the current thread.
     *
     * @param context The {@link Context} to be set.
     */
    public static void setContext(Context context) {
        contextThreadLocal.set(context);
    }

    /**
     * Retrieves the {@link Context} associated with the current thread.
     *
     * @return The {@link Context} for the current thread, or {@code null} if none is set.
     */
    public static Context context() {
        return contextThreadLocal.get();
    }

    /**
     * Clears the {@link Context} for the current thread.
     * This method removes the {@link Context} from the thread-local storage.
     */
    public static void clearContext() {
        contextThreadLocal.remove();
    }
}