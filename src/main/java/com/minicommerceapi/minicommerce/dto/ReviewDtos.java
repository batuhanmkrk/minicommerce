package com.minicommerceapi.minicommerce.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ReviewDtos {
    public record CreateReviewRequest(
            @NotNull Long userId,
            @NotNull Long productId,
            @Min(1) @Max(5) int rating,
            @Size(max = 600) String comment
    ) {}

    public record ReviewResponse(
            Long id,
            Long userId,
            Long productId,
            int rating,
            String comment
    ) {}

public record PatchReviewRequest(
        @Min(1) @Max(5) Integer rating,
        @Size(max = 600) String comment
) {}

}
