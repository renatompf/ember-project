package io.ember.examples.ServiceExample.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record UserDTO(
        @JsonProperty(value = "name")
        String name,
        @JsonProperty(value = "years_old")
        Integer age,
        @JsonProperty(value = "email")
        String email
) {
}
