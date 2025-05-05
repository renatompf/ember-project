package io.github.renatompf.ember.annotations.http;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark a method as a GET HTTP request handler.
 * <p>
 * This annotation can be used to define a GET route in a controller.
 * The value of the annotation specifies the path for the GET request.
 * </p>
 * <p>
 * Example usage:
 * </p>
 * <pre>
 * {@code
 * @Controller("/users")
 * public class UserController {
 *
 *     @Get
 *     public Response getUsers() {
 *         // Logic to get users
 *         return Response.ok().build();
 *     }
 * }
 * }
 * </pre>
 */
@Target( { ElementType.METHOD })
@Retention( RetentionPolicy.RUNTIME)
public @interface Get {
    /**
     * The path for the GET request.
     * This path will be used to match incoming GET requests to this method.
     *
     * @return The path for the GET request.
     */
    String value() default "";
}
