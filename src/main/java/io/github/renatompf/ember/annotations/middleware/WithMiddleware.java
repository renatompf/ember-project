package io.github.renatompf.ember.annotations.middleware;

import io.github.renatompf.ember.core.Middleware;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface WithMiddleware {
    Class<? extends Middleware>[] value();
}
