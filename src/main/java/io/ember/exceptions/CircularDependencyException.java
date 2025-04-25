package io.ember.exceptions;

public class CircularDependencyException extends RuntimeException {

    public CircularDependencyException(){
        super();
    }

    public CircularDependencyException(String message) {
        super(message);
    }
}