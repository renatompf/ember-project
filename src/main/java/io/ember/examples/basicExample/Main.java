package io.ember.examples.basicExample;

import io.ember.EmberApplication;
import io.ember.examples.basicExample.dto.EchoRequest;

import java.util.Map;

public class Main {
    public static void main(String[] args) {
        EmberApplication app = new EmberApplication();

        app
                .get("/ping", ctx -> ctx.ok(Map.of("message", "pong")))
                .post("/echo", ctx -> {
                    EchoRequest requestBody = ctx.bodyAs(EchoRequest.class);
                    ctx.ok(Map.of("received", requestBody));
                })
                .get("/hello/:name?", ctx -> {
                    String name = ctx.pathParam("name");
                    String greeting = name == null ? "Hello, World!" : "Hello, " + name + "!";
                    ctx.ok(Map.of("greeting", greeting));
                })
                .group("/admin", admin -> {
                    admin
                            .use(ctx -> {
                                System.out.println("[GROUP] Auth check...");
                                String token = ctx.queryParam("token");
                                if (!"admin".equals(token)) {
                                    ctx.forbidden("Forbidden");
                                }
                                ctx.next();
                            })
                            .get("/dashboard", ctx ->
                                    ctx.ok(Map.of("message", "Welcome to admin dashboard"))
                            );
                })
                .get("/about", ctx ->
                        ctx.ok(Map.of("info", "About us"))
                );

        app.start(8080);
    }
}