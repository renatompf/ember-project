package io.github.renatompf.ember.annotations.http;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark a method as a POST HTTP request handler.
 * <p>
 * This annotation can be used to define a POST route in a controller.
 * The value of the annotation specifies the path for the POST request.
 * </p>
 * <p>
 * Example usage:
 * </p>
 * <pre>
 * {@code
 * @Controller("/users")
 * public class UserController {
 *
 *     @Post("/create")
 *     public Response createUser() {
 *         // Logic to create user
 *         return Response.ok().build();
 *     }
 * }
 * }
 * </pre>
 */
@Target( { ElementType.METHOD })
@Retention( RetentionPolicy.RUNTIME)
public @interface Post {
    /**
     * The path for the POST request.
     * This path will be used to match incoming POST requests to this method.
     *
     * @return The path for the POST request.
     */
    String value() default "";
}
