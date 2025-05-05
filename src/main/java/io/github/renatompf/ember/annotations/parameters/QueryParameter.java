package io.github.renatompf.ember.annotations.parameters;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark a method parameter as a query parameter in a RESTful API.
 * <p>
 * This annotation can be used to extract values from the query string and pass them as method parameters.
 * </p>
 * <p>
 * Example usage:
 * </p>
 * <pre>
 * {@code
 * @Get("/users")
 * public Response getUsers(@QueryParameter("age") int age) {
 *     // Logic to get users by age
 *     return Response.ok().build();
 * }
 * }
 * </pre>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface QueryParameter {
    /**
     * The name of the query parameter.
     * This name should match the key in the query string.
     *
     * @return The name of the query parameter.
     */
    String value();
}