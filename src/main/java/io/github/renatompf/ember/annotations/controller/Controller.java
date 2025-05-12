package io.github.renatompf.ember.annotations.controller;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Annotation to mark a class as a Controller in the Ember framework.
 * This annotation can be used to define a base path for the controller.
 * The default value is an empty string, which means the controller will not have a specific base path.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Controller {
    /**
     * The base path for the controller.
     * This path will be used as a prefix for all routes defined in this controller.
     *
     * @return The base path for the controller.
     */
    String value() default "/"; // The base path for the controller, default is an empty string
}
