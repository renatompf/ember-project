package io.github.renatompf.ember.exceptions;

import io.github.renatompf.ember.enums.HttpStatusCode;

/**
 * Represents an HTTP exception that can be thrown during request processing.
 * <p>
 * This class encapsulates the HTTP status code and the response body that should be returned
 * to the client in case of an error.
 * </p>
 */
public class HttpException extends RuntimeException {

    /**
     * The HTTP status code associated with this exception.
     */
    private final HttpStatusCode status;

    /**
     * The response body that should be returned to the client.
     */
    private final Object body;

    /**
     * Constructs a new HttpException with the specified status code and response body.
     *
     * @param status The HTTP status code.
     * @param body   The response body.
     */
    public HttpException(HttpStatusCode status, Object body) {
        super(body instanceof String ? (String) body : null);
        this.status = status;
        this.body = body;
    }

    /**
     * Retrieves the HTTP status code associated with this exception.
     *
     * @return The HTTP status code as a {@code HttpStatusCode}.
     */
    public HttpStatusCode getStatus() {
        return status;
    }

    /**
     * Gets the response body that should be returned to the client.
     *
     * @return The response body.
     */
    public Object getBody() {
        return body;
    }
}
