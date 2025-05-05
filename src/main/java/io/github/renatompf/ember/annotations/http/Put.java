package io.github.renatompf.ember.annotations.http;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark a method as a PUT HTTP request handler.
 * <p>
 * This annotation can be used to define a PUT route in a controller.
 * The value of the annotation specifies the path for the PUT request.
 * </p>
 * <p>
 * Example usage:
 * </p>
 * <pre>
 * {@code
 * @Controller("/users")
 * public class UserController {
 *
 *     @Put("/update")
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
public @interface Put {
    /**
     * The path for the PUT request.
     * This path will be used to match incoming PUT requests to this method.
     *
     * @return The path for the PUT request.
     */
    String value() default "";
}
