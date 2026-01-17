package com.minicommerceapi.minicommerce.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public class OrderDtos {

    public record CreateOrderItem(
            @NotNull Long productId,
            @Min(1) int quantity
    ) {}

    public record CreateOrderRequest(
            @NotNull Long userId,
            @NotNull @Size(min = 1) List<@Valid CreateOrderItem> items
    ) {}

    public record OrderItemResponse(
            Long productId,
            String productName,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal lineTotal
    ) {}

    public record OrderResponse(
            Long id,
            Long userId,
            String status,
            BigDecimal total,
            List<OrderItemResponse> items
    ) {}

public record PatchOrderRequest(
        @NotNull String status
) {}

}
