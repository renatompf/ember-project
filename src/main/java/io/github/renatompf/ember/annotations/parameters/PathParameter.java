package io.github.renatompf.ember.annotations.parameters;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark a method parameter as a path parameter in a RESTful API.
 * <p>
 * This annotation can be used to extract values from the URL path and pass them as method parameters.
 * </p>
 * <p>
 * Example usage:
 * </p>
 * <pre>
 * {@code
 * @Get("/users/:id")
 * public Response getUser(@PathParameter("id") String userId) {
 *     // Logic to get user by ID
 *     return Response.ok().build();
 * }
 * }
 * </pre>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface PathParameter {
    /**
     * The name of the path parameter.
     * This name should match the placeholder in the URL pattern.
     *
     * @return The name of the path parameter.
     */
    String value();

}
