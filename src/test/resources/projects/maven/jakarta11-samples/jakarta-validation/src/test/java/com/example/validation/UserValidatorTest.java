package com.example.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;

class UserValidatorTest {

    private final UserValidator validator = new UserValidator();

    @Test
    void validUserHasNoViolations() {
        assertTrue(validator.validate(new User("Alice", "alice@example.com", 30)).isEmpty());
    }

    @Test
    void invalidUserReportsAllViolations() {
        Set<String> violations = validator.validate(new User(" ", "not-an-email", 10));
        assertEquals(Set.of(
                "age: age must be at least 18",
                "email: email must be valid",
                "name: name must not be blank"), violations);
    }
}
