package io.github.renatompf.ember.annotations.http;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark a method as a DELETE HTTP request handler.
 * <p>
 * This annotation can be used to define a DELETE route in a controller.
 * The value of the annotation specifies the path for the DELETE request.
 * </p>
 * <p>
 * Example usage:
 * </p>
 * <pre>
 * {@code
 * @Controller("/users")
 * public class UserController {
 *
 *     @Delete("/delete")
 *     public Response deleteUser() {
 *         // Logic to delete user
 *         return Response.ok().build();
 *     }
 * }
 * }
 * </pre>
 */
@Target( { ElementType.METHOD })
@Retention( RetentionPolicy.RUNTIME)
public @interface Delete {
    /**
     * The path for the DELETE request.
     * This path will be used to match incoming DELETE requests to this method.
     *
     * @return The path for the DELETE request.
     */
    String value() default "";
}
