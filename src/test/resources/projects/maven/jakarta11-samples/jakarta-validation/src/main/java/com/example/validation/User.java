package com.example.validation;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class User {

    @NotBlank(message = "name must not be blank")
    @Size(max = 50, message = "name must be at most 50 characters")
    private final String name;

    @Email(message = "email must be valid")
    @NotBlank(message = "email must not be blank")
    private final String email;

    @Min(value = 18, message = "age must be at least 18")
    private final int age;

    public User(String name, String email, int age) {
        this.name = name;
        this.email = email;
        this.age = age;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public int getAge() {
        return age;
    }
}
