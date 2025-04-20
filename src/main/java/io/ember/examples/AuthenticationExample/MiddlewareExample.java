package io.ember.examples.AuthenticationExample;

import io.ember.EmberApplication;
import io.ember.examples.AuthenticationExample.middleware.AuthMiddleware;

public class MiddlewareExample {

    public static void main(String[] args) {
        EmberApplication app = new EmberApplication();

        // Add authentication middleware with a secret key
        app.use(new AuthMiddleware("mySecretKeymySecretKeymySecretKeymySecretKey"));

        // Define a protected route
        app.get("/protected", ctx -> {
            // This route is protected by the AuthMiddleware
            // If the token is valid, the user will be able to access this route
            // Otherwise, they will receive a 401 Unauthorized response
            ctx.ok("Welcome to a protected route!");
        });

        app.start(8080);
    }
}
