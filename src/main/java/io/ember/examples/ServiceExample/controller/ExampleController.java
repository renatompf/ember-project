package io.ember.examples.ServiceExample.controller;

import io.ember.annotations.controller.Controller;
import io.ember.annotations.http.Get;
import io.ember.core.Context;
import io.ember.core.ContextHolder;
import io.ember.examples.ServiceExample.ServiceExample;
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
    public void getUserById() {
        Integer id =  ContextHolder.context().pathParams().pathParamAs("id", Integer::parseInt);
        ContextHolder.context().response().ok(userService.getUserById(id));
    }

    @Get("/list")
    public void listAllUsers() {
        ContextHolder.context().response().ok(String.join(", ", userService.listAllUsers()));
    }

}
