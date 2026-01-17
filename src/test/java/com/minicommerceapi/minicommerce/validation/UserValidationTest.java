package com.minicommerceapi.minicommerce.validation;

import com.minicommerceapi.minicommerce.dto.UserDtos;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class UserValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void init() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void close() {
        factory.close();
    }

    @Test
    void createUser_shouldFail_whenNameBlank() {
        UserDtos.CreateUserRequest req = new UserDtos.CreateUserRequest("", "ali@test.com");
        Set<ConstraintViolation<UserDtos.CreateUserRequest>> v = validator.validate(req);
        assertFalse(v.isEmpty());
        assertTrue(v.stream().anyMatch(cv -> cv.getPropertyPath().toString().equals("name")));
    }

    @Test
    void createUser_shouldFail_whenEmailInvalid() {
        UserDtos.CreateUserRequest req = new UserDtos.CreateUserRequest("Ali", "not-an-email");
        Set<ConstraintViolation<UserDtos.CreateUserRequest>> v = validator.validate(req);
        assertFalse(v.isEmpty());
        assertTrue(v.stream().anyMatch(cv -> cv.getPropertyPath().toString().equals("email")));
    }

    @Test
    void createUser_shouldPass_whenValid() {
        UserDtos.CreateUserRequest req = new UserDtos.CreateUserRequest("Ali", "ali@test.com");
        Set<ConstraintViolation<UserDtos.CreateUserRequest>> v = validator.validate(req);
        assertTrue(v.isEmpty());
    }
}
