package io.github.renatompf.ember.core;

import io.github.renatompf.ember.enums.HttpStatusCode;
import io.github.renatompf.ember.enums.MediaType;

/**
 * Represents an HTTP response with a status code, content type, and body.
 *
 * @param <T> The type of the response body.
 */
public class Response<T> {
    private final HttpStatusCode statusCode;
    private final T body;
    private String contentType = MediaType.APPLICATION_JSON.getType();

    /**
     * Constructs a new Response instance.
     *
     * @param statusCode  The HTTP status code of the response.
     * @param contentType The content type of the response.
     * @param body        The body of the response.
     */
    private Response(HttpStatusCode statusCode, String contentType, T body) {
        this.statusCode = statusCode;
        this.contentType = contentType;
        this.body = body;
    }

    /**
     * Creates a new Builder instance for constructing a Response.
     *
     * @param <T>        The type of the response body.
     * @param statusCode The HTTP status code for the response.
     * @return A new Builder instance.
     */
    public static <T> Builder<T> status(HttpStatusCode statusCode) {
        return new Builder<>(statusCode);
    }

    /**
     * Creates a new Builder instance for constructing a Response with a 200 OK status.
     *
     * @param <T> The type of the response body.
     * @return A new Builder instance with a 200 OK status.
     */
    public static <T> Builder<T> ok() {
        return new Builder<>(HttpStatusCode.OK);
    }

    /**
     * Creates a new Builder instance for constructing a Response with a 201 Created status.
     *
     * @param <T> The type of the response body.
     * @return A new Builder instance with a 201 Created status.
     */
    public static <T> Builder<T> created() {
        return new Builder<>(HttpStatusCode.CREATED);
    }

    /**
     * Creates a new Builder instance for constructing a Response with a 204 No Content status.
     *
     * @param <T> The type of the response body.
     * @return A new Builder instance with a 204 No Content status.
     */
    public static <T> Builder<T> badRequest() {
        return new Builder<>(HttpStatusCode.BAD_REQUEST);
    }

    /**
     * Creates a new Builder instance for constructing a Response with a 404 Not Found status.
     *
     * @param <T> The type of the response body.
     * @return A new Builder instance with a 404 Not Found status.
     */
    public static <T> Builder<T> notFound() {
        return new Builder<>(HttpStatusCode.NOT_FOUND);
    }

    /**
     * Creates a new Builder instance for constructing a Response with a 204 No Content status.
     *
     * @param <T> The type of the response body.
     * @return A new Builder instance with a 204 No Content status.
     */
    public static <T> Builder<T> noContent() {
        return new Builder<>(HttpStatusCode.NO_CONTENT);
    }

    /**
     * Creates a new Builder instance for constructing a Response with a 401 Unauthorized status.
     *
     * @param <T> The type of the response body.
     * @return A new Builder instance with a 401 Unauthorized status.
     */
    public static <T> Builder<T> unauthorized() {
        return new Builder<>(HttpStatusCode.UNAUTHORIZED);
    }

    /**
     * Creates a new Builder instance for constructing a Response with a 403 Forbidden status.
     *
     * @param <T> The type of the response body.
     * @return A new Builder instance with a 403 Forbidden status.
     */
    public static <T> Builder<T> forbidden() {
        return new Builder<>(HttpStatusCode.FORBIDDEN);
    }

    /**
     * Creates a new Builder instance for constructing a Response with a 500 Internal Server Error status.
     *
     * @param <T> The type of the response body.
     * @return A new Builder instance with a 500 Internal Server Error status.
     */
    public static <T> Builder<T> internalServerError() {
        return new Builder<>(HttpStatusCode.INTERNAL_SERVER_ERROR);
    }

    /**
     * Retrieves the HTTP status code of the response.
     *
     * @return The HTTP status code.
     */
    public HttpStatusCode getStatusCode() {
        return statusCode;
    }

    /**
     * Retrieves the content type of the response.
     *
     * @return The content type as a string.
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * Retrieves the body of the response.
     *
     * @return The body of the response.
     */
    public T getBody() {
        return body;
    }

    /**
     * Converts the response to a string representation.
     *
     * @return A string representation of the response.
     */
    @Override
    public String toString() {
        return "Response{" +
                "statusCode=" + statusCode +
                ", contentType='" + contentType + '\'' +
                ", body=" + body +
                '}';
    }

    /**
     * A builder class for constructing instances of {@link Response}.
     *
     * @param <T> The type of the response body.
     */
    public static class Builder<T> {
        private final HttpStatusCode statusCode;
        private MediaType contentType = MediaType.APPLICATION_JSON;
        private T body;

        /**
         * Constructs a new Builder instance with the specified HTTP status code.
         *
         * @param statusCode The HTTP status code for the response.
         */
        private Builder(HttpStatusCode statusCode) {
            this.statusCode = statusCode;
        }

        /**
         * Sets the content type for the response.
         *
         * @param contentType The content type to set.
         * @return The current Builder instance for method chaining.
         */
        public Builder<T> contentType(MediaType contentType) {
            this.contentType = contentType;
            return this;
        }

        /**
         * Sets the body for the response and returns a new Builder instance
         * with the updated body type.
         *
         * @param <R>  The type of the new response body.
         * @param body The body to set.
         * @return A new Builder instance with the updated body type.
         */
        public <R> Builder<R> body(R body) {
            Builder<R> newBuilder = new Builder<>(this.statusCode);
            newBuilder.contentType(this.contentType);
            newBuilder.body = body;
            return newBuilder;
        }

        /**
         * Builds and returns a new {@link Response} instance.
         *
         * @return A new Response instance.
         */
        public Response<T> build() {
            return new Response<>(statusCode, contentType.getType(), body);
        }
    }
}