package io.ember.examples.ServiceExample.controller;

import io.ember.annotations.controller.Controller;
import io.ember.annotations.http.Get;
import io.ember.annotations.parameters.PathParameter;
import io.ember.core.ContextHolder;
import io.ember.examples.ServiceExample.service.UserService;

@Controller("/example")
public class ExampleController {

    private final UserService userService;

    public ExampleController(UserService userService) {
        this.userService = userService;
    }

    @Get("/get")
    public void getUser() {
        ContextHolder.context().response().ok(userService.getUser());
    }

    @Get("/user/:id")
    public void getUserById(@PathParameter("id") Integer id) {
        if (id == null) {
            ContextHolder.context().response().badRequest("ID cannot be null");
            return;
        }
        System.out.println("ID: " + id);
        ContextHolder.context().response().ok(userService.getUserById(id));
    }

    @Get("/list")
    public void listAllUsers() {
        ContextHolder.context().response().ok(String.join(", ", userService.listAllUsers()));
    }

}
