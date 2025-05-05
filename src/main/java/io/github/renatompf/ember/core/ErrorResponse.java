package io.github.renatompf.ember.core;

import io.github.renatompf.ember.enums.HttpStatusCode;

import java.time.LocalDateTime;

/**
 * Represents an error response that can be returned to the client.
 * <p>
 * This class encapsulates the details of an error, including the HTTP status code,
 * error message, request path, timestamp, and exception type.
 * </p>
 */
public class ErrorResponse {
    private final HttpStatusCode status;
    private final String message;
    private final String path;
    private final LocalDateTime timestamp;
    private final String exception;

    /**
     * Constructs an ErrorResponse with the specified status code.
     *
     * @param status The HTTP status code.
     */
    public ErrorResponse(HttpStatusCode status) {
        this.status = status;
        this.message = status.getMessage();
        this.path = null;
        this.timestamp = LocalDateTime.now();
        this.exception = null;
    }

    /**
     * Constructs an ErrorResponse with the specified status code and message.
     *
     * @param status  The HTTP status code.
     * @param message The error message.
     */
    public ErrorResponse(HttpStatusCode status, String message) {
        this.status = status;
        this.message = message;
        this.path = null;
        this.timestamp = LocalDateTime.now();
        this.exception = null;
    }

    /**
     * Constructs an ErrorResponse with the specified status code, message, and request path.
     *
     * @param status  The HTTP status code.
     * @param message The error message.
     * @param path    The request path that caused the error.
     */
    public ErrorResponse(HttpStatusCode status, String message, String path) {
        this.status = status;
        this.message = message;
        this.path = path;
        this.timestamp = LocalDateTime.now();
        this.exception = null;
    }

    /**
     * Constructs an ErrorResponse with the specified status code, message, and exception.
     *
     * @param status     The HTTP status code.
     * @param message    The error message.
     * @param exception  The exception that caused the error.
     */
    public ErrorResponse(HttpStatusCode status, String message, Exception exception) {
        this.status = status;
        this.message = message;
        this.path = null;
        this.timestamp = LocalDateTime.now();
        this.exception = exception.getClass().getName();
    }

    /**
     * Constructs an ErrorResponse with the specified status code, message, request path, and exception.
     *
     * @param status     The HTTP status code.
     * @param message    The error message.
     * @param path       The request path that caused the error.
     * @param exception  The exception that caused the error.
     */
    public ErrorResponse(HttpStatusCode status, String message, String path, String exception) {
        this.status = status;
        this.message = message;
        this.path = path;
        this.timestamp = LocalDateTime.now();
        this.exception = exception;
    }

    /**
     * Returns the HTTP status code.
     *
     * @return The HTTP status code as an HttpStatusCode object.
     */
    public HttpStatusCode getStatus() {
        return status;
    }

    /**
     * Returns the error message.
     *
     * @return The error message as a string.
     */
    public String getMessage() {
        return message;
    }

    /**
     * Returns the request path that caused the error.
     *
     * @return The request path as a string.
     */
    public String getPath() {
        return path;
    }

    /**
     * Returns the timestamp of when the error occurred.
     *
     * @return The timestamp as a LocalDateTime object.
     */
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    /**
     * Returns the exception type that caused the error.
     *
     * @return The exception type as a string.
     */
    public String getException() {
        return exception;
    }

    /**
     * Returns a string representation of the ErrorResponse object.
     *
     * @return A string representation of the ErrorResponse object.
     */
    @Override
    public String toString() {
        return "ErrorResponse{" +
                "status=" + status +
                ", message='" + message + '\'' +
                ", path='" + path + '\'' +
                ", timestamp=" + timestamp +
                ", exception='" + exception + '\'' +
                '}';
    }
}
