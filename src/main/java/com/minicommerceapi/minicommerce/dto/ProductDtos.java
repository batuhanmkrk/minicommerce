package com.minicommerceapi.minicommerce.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public class ProductDtos {
    public record CreateProductRequest(
            @NotBlank @Size(max = 120) String name,
            @NotBlank @Size(max = 40) String sku,
            @NotNull @DecimalMin(value = "0.00", inclusive = false) BigDecimal price,
            @Min(0) int stock,
            @NotNull Long categoryId
    ) {}

    public record PatchProductRequest(
            @Size(max = 120) String name,
            @Size(max = 40) String sku,
            @DecimalMin(value = "0.00", inclusive = false) BigDecimal price,
            Integer stock,
            Long categoryId
    ) {}

    public record ProductResponse(
            Long id,
            String name,
            String sku,
            BigDecimal price,
            int stock,
            Long categoryId,
            String categoryName
    ) {}
}
