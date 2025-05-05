package io.github.renatompf.ember.annotations.middleware;

import io.github.renatompf.ember.core.Middleware;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to specify middleware classes for a controller or method.
 * <p>
 * This annotation can be used to apply middleware to a specific controller or method.
 * The middleware classes specified in the value will be applied in the order they are listed.
 * </p>
 * <p>
 * Example usage:
 * </p>
 * <pre>
 * {@code
 * @Controller("/users")
 * @WithMiddleware({AuthMiddleware.class})
 * public class UserController {
 *
 *     @Get
*      @WithMiddleware(LoggingMiddleware.class)
 *     public Response getUsers() {
 *         // Logic to get users
 *         return Response.ok().build();
 *     }
 * }
 * }
 * </pre>
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface WithMiddleware {
    /**
     * The middleware classes to be applied.
     * These classes must extend the Middleware class.
     *
     * @return The middleware classes to be applied.
     */
    Class<? extends Middleware>[] value();
}
