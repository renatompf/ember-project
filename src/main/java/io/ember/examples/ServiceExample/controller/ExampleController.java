package io.ember.examples.ServiceExample.controller;

import io.ember.annotations.controller.Controller;
import io.ember.annotations.http.Get;
import io.ember.annotations.http.Post;
import io.ember.annotations.middleware.WithMiddleware;
import io.ember.annotations.parameters.PathParameter;
import io.ember.annotations.parameters.QueryParameter;
import io.ember.annotations.parameters.RequestBody;
import io.ember.core.Response;
import io.ember.examples.ServiceExample.dto.UserDTO;
import io.ember.examples.ServiceExample.middleware.CustomMiddleware;
import io.ember.examples.ServiceExample.middleware.GlobalCustomMiddleware;
import io.ember.examples.ServiceExample.service.UserService;

import java.util.Arrays;
import java.util.stream.Collectors;

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
    public Response getUser() {
        return Response.ok(userService.getUser());
    }

    @Get("/user/:id")
    public Response getUserById(@PathParameter("id") Integer id) {
        if (id == null) {
            return Response.badRequest("ID cannot be null");
        }

        System.out.println("ID: " + id);
        return Response.ok(userService.getUserById(id));
    }

    @Get("/:id?")
    public Response getUserByIdOptional(@PathParameter("id") Integer id) {
        return Response.ok(userService.getUserById(id));
    }

    @Get("/list")
    public Response listAllUsers() {
        return Response.ok(Arrays.stream(userService.listAllUsers()).collect(
                Collectors.toMap(
                        user -> user,
                        String::length
        )));
    }

    @Get("/search")
    public Response searchUsers(@QueryParameter("name") String name, @QueryParameter("age") Integer age) {
        String response = "Searching for users with name: " + name + " and age: " + age;
        return Response.ok(response);
    }

    @Post
    public Response createUser(@RequestBody UserDTO body) {
        if (body == null) {
            return Response.badRequest("Body cannot be null");
        }

        return Response.ok(userService.createUser(body));
    }

}
