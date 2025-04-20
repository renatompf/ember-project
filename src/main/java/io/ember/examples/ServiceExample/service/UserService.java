package io.ember.examples.ServiceExample.service;

import io.ember.annotations.service.Service;

import java.util.List;

@Service
public class UserService {


    public String getUser() {
        return "User";
    }

    public String getUserById(Integer id) {
        return "User with ID: " + id;
    }

    public String[] listAllUsers() {
        return new String[] {"Lisa", "John", "Harry", "Sally"};
    }

    public String createUser(String user) {
        return "Created user: " + user;
    }

    public String updateUser(String id, String user) {
        return "Updated user with ID: " + id + " to " + user;
    }

    public String deleteUser(String id) {
        return "Deleted user with ID: " + id;
    }

}
