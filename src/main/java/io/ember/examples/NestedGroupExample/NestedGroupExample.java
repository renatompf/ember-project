package io.ember.examples.NestedGroupExample;

import io.ember.EmberApplication;
import io.ember.core.Middleware;
import io.ember.examples.NestedGroupExample.dto.EchoRequest;

public class NestedGroupExample {
    public static void main(String[] args) {
        EmberApplication app = new EmberApplication();

        Middleware loggingMiddleware = ctx -> {
            System.out.println("[LOG] " + ctx.getMethod() + " " + ctx.getPath());
            ctx.next(); // continue to next middleware or route handler
        };


        Middleware authMiddleware = ctx -> {
            String token = ctx.queryParams().queryParam("token");
            if (!"admin".equals(token)) {
                ctx.response().forbidden("Forbidden");
            }
            ctx.next(); // continue to next middleware or route handler
        };

        app.group("/api", api -> {
            api.use(loggingMiddleware);

            api.get("/ping", ctx -> ctx.response().ok("pong"));

            api.group("/admin", admin -> {
                admin.use(authMiddleware);

                admin.get("/dashboard", ctx -> ctx.response().ok("admin dashboard"));

                admin.group("/users", users -> {
                    users.get("/", ctx -> ctx.response().ok("user list"));
                    users.post("/", ctx -> {
                        EchoRequest echoRequest = ctx.body().parseBodyAs(EchoRequest.class);
                        ctx.response().ok(echoRequest);
                    });
                });
            });
        });

        app.start(8080);
    }

}
