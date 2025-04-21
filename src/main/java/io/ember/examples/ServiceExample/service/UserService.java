package io.ember.examples.ServiceExample.service;

import io.ember.annotations.service.Service;
import io.ember.examples.ServiceExample.dto.UserDTO;

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

    public String createUser(UserDTO user) {
        System.out.println("User name: " + user.name());
        System.out.println("User age: " + user.age());
        System.out.println("User email: " + user.email());

        return "Created user: " + user.name() + ", age: " + user.age();
    }

    public String updateUser(String id, String user) {
        return "Updated user with ID: " + id + " to " + user;
    }

    public String deleteUser(String id) {
        return "Deleted user with ID: " + id;
    }

}
