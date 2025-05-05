package io.github.renatompf.ember.annotations.service;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark a class as a Service in the Ember framework.
 * <p>
 * This annotation can be used to define a service that can be injected into other components.
 * </p>
 * <p>
 * Example usage:
 * </p>
 * <pre>
 * {@code
 * @Service
 * public class MyService {
 *     // Service methods
 * }
 * }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Service {
}
