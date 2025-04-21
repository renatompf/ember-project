package io.ember.core;

public record Response(int statusCode, Object body) {

    public static Response ok(Object body) {
        return new Response(200, body);
    }

    public static Response created(Object body) {
        return new Response(201, body);
    }

    public static Response noContent() {
        return new Response(204, null);
    }

    public static Response badRequest(Object body) {
        return new Response(400, body);
    }

    public static Response notFound(Object body) {
        return new Response(404, body);
    }

    public static Response internalServerError(Object body) {
        return new Response(500, body);
    }

    public static Response unauthorized(Object body) {
        return new Response(401, body);
    }

    public static Response forbidden(Object body) {
        return new Response(403, body);
    }

}
