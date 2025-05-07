package io.github.renatompf.ember.annotations.content;

import io.github.renatompf.ember.enums.MediaType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to specify the media types that a method can produce.
 * <p>
 * This annotation is used to indicate the media types that a method can return
 * in its response body. It is typically used in conjunction with HTTP methods
 * such as GET or POST.
 * </p>
 * <p>
 * Example usage:
 * </p>
 * <pre>
 * {@code
 * @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
 * public MyResponse myMethod() {
 *     // Method implementation
 * }
 * }
 * </pre>
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Produces {
    /**
     * The media types that the method can produce.
     *
     * @return an array of media types
     */
    MediaType[] value() default {MediaType.APPLICATION_JSON};
}
