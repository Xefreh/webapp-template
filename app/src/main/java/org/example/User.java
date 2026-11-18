package org.example;

import com.fasterxml.jackson.annotation.JsonIgnore;

public record User(Integer id, String name, String email,  @JsonIgnore String passwordHash) {

    public User(String name, String email, String passwordHash) {
        this(null, name, email, passwordHash);
    }
}
