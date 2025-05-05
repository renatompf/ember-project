package io.github.renatompf.ember.enums;

/**
 * Enum representing HTTP methods.
 * <p>
 * This enum defines the standard HTTP methods used in web applications.
 * </p>
 * <p>
 * Example usage:
 * </p>
 * <pre>
 * {@code
 * HttpMethod method = HttpMethod.GET;
 * }
 * </pre>
 */
public enum HttpMethod {

    /**
     * HTTP GET method.
     * <p>
     * Used to retrieve data from the server.
     * </p>
     */
    GET("GET"),
    /**
     * HTTP POST method.
     * <p>
     * Used to send data to the server.
     * </p>
     */
    POST("POST"),
    /**
     * HTTP PUT method.
     * <p>
     * Used to update data on the server.
     * </p>
     */
    PUT("PUT"),
    /**
     * HTTP DELETE method.
     * <p>
     * Used to delete data from the server.
     * </p>
     */
    DELETE("DELETE"),
    /**
     * HTTP PATCH method.
     * <p>
     * Used to apply partial modifications to a resource.
     * </p>
     */
    PATCH("PATCH"),
    /**
     * HTTP OPTIONS method.
     * <p>
     * Used to describe the communication options for the target resource.
     * </p>
     */
    OPTIONS("OPTIONS"),
    /**
     * HTTP HEAD method.
     * <p>
     * Used to retrieve the headers of a resource without the body.
     * </p>
     */
    HEAD("HEAD");

    /**
     * The HTTP method as a string.
     */
    private final String method;

    /**
     * Array of allowed HTTP methods.
     * <p>
     * This array contains the standard HTTP methods that are allowed in the application.
     * </p>
     */
    private static final String[] ALLOWED_METHODS = {"GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS", "HEAD"};

    /**
     * Constructor for HttpMethod enum.
     *
     * @param method The HTTP method as a string.
     */
    HttpMethod(String method) {
        this.method = method;
    }

    /**
     * Retrieves the HTTP method as a string.
     *
     * @return The HTTP method as a string.
     */
    public String getMethod() {
        return method;
    }

    /**
     * Checks if the given HTTP method is allowed.
     *
     * @param method The HTTP method to check.
     * @return True if the method is allowed, false otherwise.
     */
    public static boolean isAllowedMethod(String method) {
        for (String allowedMethod : ALLOWED_METHODS) {
            if (allowedMethod.equalsIgnoreCase(method)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Converts a string to the corresponding HttpMethod enum.
     *
     * @param method The HTTP method as a string.
     * @return The corresponding HttpMethod enum.
     * @throws IllegalArgumentException if the method is not valid.
     */
    public static HttpMethod fromString(String method) {
        for (HttpMethod httpMethod : HttpMethod.values()) {
            if (httpMethod.method.equalsIgnoreCase(method)) {
                return httpMethod;
            }
        }
        throw new IllegalArgumentException("Invalid HTTP method: " + method);
    }

}
