package io.github.renatompf.ember.annotations.parameters;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to indicate that a parameter should be validated.
 * This annotation is used in conjunction with the {@link io.github.renatompf.ember.core.ValidationProvider}
 * to perform validation on the annotated parameter.
 *
 * @see io.github.renatompf.ember.core.ValidationProvider
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface Validated {

}