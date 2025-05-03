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

    public ErrorResponse(HttpStatusCode status) {
        this.status = status;
        this.message = status.getMessage();
        this.path = null;
        this.timestamp = LocalDateTime.now();
        this.exception = null;
    }

    public ErrorResponse(HttpStatusCode status, String message) {
        this.status = status;
        this.message = message;
        this.path = null;
        this.timestamp = LocalDateTime.now();
        this.exception = null;
    }

    public ErrorResponse(HttpStatusCode status, String message, String path) {
        this.status = status;
        this.message = message;
        this.path = path;
        this.timestamp = LocalDateTime.now();
        this.exception = null;
    }

    public ErrorResponse(HttpStatusCode status, String message, Exception exception) {
        this.status = status;
        this.message = message;
        this.path = null;
        this.timestamp = LocalDateTime.now();
        this.exception = exception.getClass().getName();
    }

    public ErrorResponse(HttpStatusCode status, String message, String path, String exception) {
        this.status = status;
        this.message = message;
        this.path = path;
        this.timestamp = LocalDateTime.now();
        this.exception = exception;
    }

    public HttpStatusCode getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public String getPath() {
        return path;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getException() {
        return exception;
    }

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
