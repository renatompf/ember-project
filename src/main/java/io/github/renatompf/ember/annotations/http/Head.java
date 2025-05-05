package io.github.renatompf.ember.annotations.http;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Annotation to mark a method as a HEAD HTTP request handler.
 * <p>
 * This annotation can be used to define a HEAD route in a controller.
 * The value of the annotation specifies the path for the HEAD request.
 * </p>
 * <p>
 * Example usage:
 * </p>
 * <pre>
 * {@code
 * @Controller("/users")
 * public class UserController {
 *
 *     @Head("/info")
 *     public Response getUserInfo() {
 *         // Logic to get user info
 *         return Response.ok().build();
 *     }
 * }
 * }
 * </pre>
 */
@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface Head {
    /**
     * The path for the HEAD request.
     * This path will be used to match incoming HEAD requests to this method.
     *
     * @return The path for the HEAD request.
     */
    String value() default "";
}