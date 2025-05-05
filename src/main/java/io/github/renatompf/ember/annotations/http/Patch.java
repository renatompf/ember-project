package io.github.renatompf.ember.annotations.http;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark a method as a PATCH HTTP request handler.
 * <p>
 * This annotation can be used to define a PATCH route in a controller.
 * The value of the annotation specifies the path for the PATCH request.
 * </p>
 * <p>
 * Example usage:
 * </p>
 * <pre>
 * {@code
 * @Controller("/users")
 * public class UserController {
 *
 *     @Patch("/update")
 *     public Response updateUser() {
 *         // Logic to update user
 *         return Response.ok().build();
 *     }
 * }
 * }
 * </pre>
 */

@Target( { ElementType.METHOD })
@Retention( RetentionPolicy.RUNTIME)
public @interface Patch {
    /**
     * The path for the PATCH request.
     * This path will be used to match incoming PATCH requests to this method.
     *
     * @return The path for the PATCH request.
     */
    String value() default "";
}
