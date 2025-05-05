package io.github.renatompf.ember.annotations.http;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
/**
 * Annotation to mark a method as an OPTIONS HTTP request handler.
 * <p>
 * This annotation can be used to define an OPTIONS route in a controller.
 * The value of the annotation specifies the path for the OPTIONS request.
 * </p>
 * <p>
 * Example usage:
 * </p>
 * <pre>
 * {@code
 * @Controller("/users")
 * public class UserController {
 *
 *     @Options("/info")
 *     public Response getUserInfo() {
 *         // Logic to get user info
 *         return Response.ok().build();
 *     }
 * }
 * }
 * </pre>
 */
@Target( { ElementType.METHOD })
@Retention( RetentionPolicy.RUNTIME)
public @interface Options {
    /**
     * The path for the OPTIONS request.
     * This path will be used to match incoming OPTIONS requests to this method.
     *
     * @return The path for the OPTIONS request.
     */
    String value() default "";
}
