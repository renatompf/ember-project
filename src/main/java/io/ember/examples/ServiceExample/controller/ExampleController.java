package io.ember.examples.ServiceExample.controller;

import io.ember.annotations.controller.Controller;
import io.ember.annotations.http.Get;
import io.ember.annotations.http.Post;
import io.ember.annotations.middleware.WithMiddleware;
import io.ember.annotations.parameters.PathParameter;
import io.ember.annotations.parameters.QueryParameter;
import io.ember.annotations.parameters.RequestBody;
import io.ember.core.ContextHolder;
import io.ember.examples.ServiceExample.dto.UserDTO;
import io.ember.examples.ServiceExample.middleware.CustomMiddleware;
import io.ember.examples.ServiceExample.middleware.GlobalCustomMiddleware;
import io.ember.examples.ServiceExample.service.UserService;

@Controller("/example")
@WithMiddleware({
        GlobalCustomMiddleware.class
})
public class ExampleController {

    private final UserService userService;

    public ExampleController(UserService userService) {
        this.userService = userService;
    }

    @Get("/get")
    @WithMiddleware(CustomMiddleware.class)
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

    @Get("/search")
    public void searchUsers(@QueryParameter("name") String name, @QueryParameter("age") Integer age) {
        String response = "Searching for users with name: " + name + " and age: " + age;
        ContextHolder.context().response().ok(response);
    }

    @Post
    public void createUser(@RequestBody UserDTO body) {
        if (body == null) {
            ContextHolder.context().response().badRequest("Body cannot be null");
            return;
        }
        System.out.println("UserDTO: " + body);
        ContextHolder.context().response().ok(userService.createUser(body));
    }

}
