package io.ember.examples.basicExample;

import io.ember.EmberApplication;
import io.ember.examples.basicExample.dto.EchoRequest;

import java.util.Map;

public class Main {
    public static void main(String[] args) {
        EmberApplication app = new EmberApplication();

        app
                .get("/ping", ctx -> ctx.response().ok(Map.of("message", "pong")))
                .post("/echo", ctx -> {
                    EchoRequest requestBody = ctx.body().parseBodyAs(EchoRequest.class);
                    ctx.response().ok(Map.of("received", requestBody));
                })
                .get("/hello/:name?", ctx -> {
                    String name = ctx.pathParams().pathParam("name");
                    String greeting = name == null ? "Hello, World!" : "Hello, " + name + "!";
                    ctx.response().ok(Map.of("greeting", greeting));
                })
                .group("/admin", admin -> {
                    admin
                            .use(ctx -> {
                                System.out.println("[GROUP] Auth check...");
                                String token = ctx.queryParams().queryParam("token");
                                if (!"admin".equals(token)) {
                                    ctx.response().forbidden("Forbidden");
                                }
                                ctx.next();
                            })
                            .get("/dashboard", ctx ->
                                    ctx.response().ok(Map.of("message", "Welcome to admin dashboard"))
                            );
                })
                .get("/about", ctx ->
                        ctx.response().ok(Map.of("info", "About us"))
                );

        app.start(8080);
    }
}