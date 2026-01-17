package com.minicommerceapi.minicommerce.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UserDtos {
    public record CreateUserRequest(
            @NotBlank @Size(max = 80) String name,
            @NotBlank @Email @Size(max = 200) String email
    ) {}

    public record UpdateUserRequest(
            @NotBlank @Size(max = 80) String name,
            @NotBlank @Email @Size(max = 200) String email
    ) {}

    public record UserResponse(
            Long id,
            String name,
            String email
    ) {}
}
