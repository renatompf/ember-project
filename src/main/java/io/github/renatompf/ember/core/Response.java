package io.github.renatompf.ember.core;

import io.github.renatompf.ember.enums.HttpStatusCode;
import io.github.renatompf.ember.enums.MediaType;

public class Response<T> {
    private final HttpStatusCode statusCode;
    private final T body;
    private String contentType = MediaType.APPLICATION_JSON.getType();

    private Response(HttpStatusCode statusCode, String contentType, T body) {
        this.statusCode = statusCode;
        this.contentType = contentType;
        this.body = body;
    }

    // Static factory methods for building Response
    public static <T> Builder<T> status(HttpStatusCode statusCode) {
        return new Builder<>(statusCode);
    }

    public static <T> Builder<T> ok() {
        return new Builder<>(HttpStatusCode.OK);
    }

    public static <T> Builder<T> created() {
        return new Builder<>(HttpStatusCode.CREATED);
    }

    public static <T> Builder<T> badRequest() {
        return new Builder<>(HttpStatusCode.BAD_REQUEST);
    }

    public static <T> Builder<T> notFound() {
        return new Builder<>(HttpStatusCode.NOT_FOUND);
    }

    public static <T> Builder<T> noContent() {
        return new Builder<>(HttpStatusCode.NO_CONTENT);
    }

    public static <T> Builder<T> unauthorized() {
        return new Builder<>(HttpStatusCode.UNAUTHORIZED);
    }

    public static <T> Builder<T> forbidden() {
        return new Builder<>(HttpStatusCode.FORBIDDEN);
    }

    public static <T> Builder<T> internalServerError() {
        return new Builder<>(HttpStatusCode.INTERNAL_SERVER_ERROR);
    }

    public HttpStatusCode getStatusCode() {
        return statusCode;
    }

    public String getContentType() {
        return contentType;
    }

    public T getBody() {
        return body;
    }

    @Override
    public String toString() {
        return "Response{" +
                "statusCode=" + statusCode +
                ", contentType='" + contentType + '\'' +
                ", body=" + body +
                '}';
    }

    public static class Builder<T> {
        private final HttpStatusCode statusCode;
        private MediaType contentType = MediaType.APPLICATION_JSON;
        private T body;

        private Builder(HttpStatusCode statusCode) {
            this.statusCode = statusCode;
        }

        public Builder<T> contentType(MediaType contentType) {
            this.contentType = contentType;
            return this;
        }

        public <R> Builder<R> body(R body) {
            Builder<R> newBuilder = new Builder<>(this.statusCode);
            newBuilder.contentType(this.contentType);
            newBuilder.body = body;
            return newBuilder;
        }

        public Response<T> build() {
            return new Response<>(statusCode, contentType.getType(), body);
        }
    }

}