package io.github.renatompf.ember.core;

@FunctionalInterface
public interface Middleware {
    void handle(Context context) throws Exception;
}
