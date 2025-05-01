package io.github.renatompf.ember.exceptions;

import io.github.renatompf.ember.enums.HttpStatusCode;

public class HttpException extends RuntimeException {
    private final HttpStatusCode status;
    private final Object body;

    public HttpException(HttpStatusCode status, Object body) {
        super(body instanceof String ? (String) body : null);
        this.status = status;
        this.body = body;
    }

    public HttpStatusCode getStatus() {
        return status;
    }

    public Object getBody() {
        return body;
    }
}
