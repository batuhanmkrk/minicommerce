package com.minicommerceapi.minicommerce.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CategoryDtos {
    public record CreateCategoryRequest(
            @NotBlank @Size(max = 80) String name
    ) {}

    public record UpdateCategoryRequest(
            @NotBlank @Size(max = 80) String name
    ) {}

    public record CategoryResponse(
            Long id,
            String name,
            String slug
    ) {}
}
