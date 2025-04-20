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
            String token = ctx.queryParam("token");
            if (!"admin".equals(token)) {
                ctx.forbidden("Forbidden");
            }
            ctx.next(); // continue to next middleware or route handler
        };

        app.group("/api", api -> {
            api.use(loggingMiddleware);

            api.get("/ping", ctx -> ctx.ok("pong"));

            api.group("/admin", admin -> {
                admin.use(authMiddleware);

                admin.get("/dashboard", ctx -> ctx.ok("admin dashboard"));

                admin.group("/users", users -> {
                    users.get("/", ctx -> ctx.ok("user list"));
                    users.post("/", ctx -> {
                        EchoRequest echoRequest = ctx.bodyAs(EchoRequest.class);
                        ctx.ok(echoRequest);
                    });
                });
            });
        });

        app.start(8080);
    }

}
