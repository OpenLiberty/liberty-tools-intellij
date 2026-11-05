package com.example.validation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class UserValidator {

    private final Validator validator;

    public UserValidator() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            this.validator = factory.getValidator();
        }
    }

    /** Returns sorted "property: message" strings; empty when the user is valid. */
    public Set<String> validate(User user) {
        Set<ConstraintViolation<User>> violations = validator.validate(user);
        return violations.stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.toCollection(TreeSet::new));
    }
}
