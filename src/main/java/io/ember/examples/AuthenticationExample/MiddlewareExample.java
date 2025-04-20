package io.ember.examples.AuthenticationExample;

import io.ember.EmberApplication;
import io.ember.examples.AuthenticationExample.middleware.AuthMiddleware;

public class MiddlewareExample {

    /**
     * This example demonstrates how to use middleware for authentication in an Ember application.
     * The AuthMiddleware checks for a valid token in the request headers and allows access to protected routes.
     * <p>
     * How to run it:
     * <p>
     * To use JWT for authentication, add the following dependencies to your `pom.xml` file:
     *
     * <pre>{@code
     * <properties>
     *     <jjwt.version>0.12.6</jjwt.version>
     * </properties>
     *
     * <dependencies>
     *     <dependency>
     *         <groupId>io.jsonwebtoken</groupId>
     *         <artifactId>jjwt-api</artifactId>
     *         <version>${jjwt.version}</version>
     *     </dependency>
     *     <dependency>
     *         <groupId>io.jsonwebtoken</groupId>
     *         <artifactId>jjwt-impl</artifactId>
     *         <version>${jjwt.version}</version>
     *         <scope>runtime</scope>
     *     </dependency>
     *     <dependency>
     *         <groupId>io.jsonwebtoken</groupId>
     *         <artifactId>jjwt-jackson</artifactId>
     *         <version>${jjwt.version}</version>
     *         <scope>runtime</scope>
     *     </dependency>
     * </dependencies>
     * }</pre>
     * <p>
     *
     * This example uses a custom `AuthMiddleware` class that checks for a valid JWT token in the request headers.
     * <p>
     * Please unccoment line in AuthMiddleware.java
     * <p>
     * To run the example, execute the `main` method in this class. The application will start on port 8080.
     */

    public static void main(String[] args) {
        EmberApplication app = new EmberApplication();

        // Add authentication middleware with a secret key
        app.use(new AuthMiddleware("mySecretKeymySecretKeymySecretKeymySecretKey"));

        // Define a protected route
        app.get("/protected", ctx -> {
            // This route is protected by the AuthMiddleware
            // If the token is valid, the user will be able to access this route
            // Otherwise, they will receive a 401 Unauthorized response
            ctx.response().ok("Welcome to a protected route!");
        });

        app.start(8080);
    }
}
