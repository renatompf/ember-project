package io.ember.annotations.parameters;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

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
