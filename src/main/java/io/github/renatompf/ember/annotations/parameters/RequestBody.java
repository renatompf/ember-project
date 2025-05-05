package io.github.renatompf.ember.annotations.parameters;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark a method parameter as a request body in a RESTful API.
 * <p>
 * This annotation can be used to extract the body of the HTTP request and pass it as a method parameter.
 * </p>
 * <p>
 * Example usage:
 * </p>
 * <pre>
 * {@code
 * @Post("/users")
 * public Response createUser(@RequestBody User user) {
 *     // Logic to create a new user
 *     return Response.ok().build();
 * }
 * }
 * </pre>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequestBody {
}
