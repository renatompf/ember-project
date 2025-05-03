package io.github.renatompf.ember.annotations.exceptions;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark a class as a global exception handler.
 * <p>
 * This annotation should be used on classes that handle exceptions globally.
 * </p>
 * <p>
 * Example usage:
 * </p>
 * <pre>
 * &#64;GlobalHandler
 * public class MyGlobalExceptionHandler {
 *     // Exception handling methods
 * }
 * </pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface GlobalHandler {
}
