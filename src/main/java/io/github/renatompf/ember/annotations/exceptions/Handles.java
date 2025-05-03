package io.github.renatompf.ember.annotations.exceptions;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as an exception handler for one or more specific exception types.
 * <p>
 * Methods annotated with {@code @Handles} should be declared inside a class
 * registered as a global handler (e.g., annotated with {@code @GlobalHandler} or similar),
 * and must have the following signature:
 * <pre>
 *     public Response handle(SomeException ex, Context ctx)
 * </pre>
 * or optionally:
 * <pre>
 *     public Response handle(SomeException ex)
 * </pre>
 * depending on what your framework supports.
 *
 * <p>When an exception is thrown during request processing and matches one of the types
 * declared in this annotation, the framework will invoke the corresponding handler method.
 *
 * <p>Example usage:
 * <pre>
 * {@code
 * @GlobalHandler
 * public class ErrorHandler {
 *
 *     @Handles({NotFoundException.class})
 *     public Response handleNotFound(NotFoundException ex) {
 *         return Response.status(404).body("Not Found: " + ex.getMessage()).build();
 *     }
 *
 *     @Handles({ValidationException.class})
 *     public Response handleValidationError(ValidationException ex, Context ctx) {
 *         return Response.status(400).body("Invalid: " + ex.getErrors()).build();
 *     }
 * }
 * }
 * </pre>
 *
 * @see java.lang.Throwable
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Handles {
    Class<? extends Throwable>[] value();
}
