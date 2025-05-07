package io.github.renatompf.ember.annotations.content;

import io.github.renatompf.ember.enums.MediaType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to specify the media types that a method can consume.
 * <p>
 * This annotation is used to indicate the media types that a method can accept
 * in its request body. It is typically used in conjunction with HTTP methods
 * such as POST or PUT.
 * </p>
 * <p>
 * Example usage:
 * </p>
 * <pre>
 * {@code
 * @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
 * public void myMethod(MyRequest request) {
 *     // Method implementation
 * }
 * }
 * </pre>
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Consumes {
    /**
     * The media types that the method can consume.
     *
     * @return an array of media types
     */
    MediaType[] value() default {MediaType.APPLICATION_JSON};
}
