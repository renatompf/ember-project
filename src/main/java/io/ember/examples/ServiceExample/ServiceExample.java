package io.ember.examples.ServiceExample;

import io.ember.EmberApplication;
import io.ember.examples.ServiceExample.service.UserService;

public class ServiceExample {

    public static void main(String[] args) {
        EmberApplication app = new EmberApplication();
        app.start(8080);
    }
}
