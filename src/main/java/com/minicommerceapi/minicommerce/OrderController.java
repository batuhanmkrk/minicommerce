package com.minicommerceapi.minicommerce;

import com.minicommerceapi.minicommerce.dto.OrderDtos;
import com.minicommerceapi.minicommerce.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @Operation(summary = "Create an order (decreases product stock)")
    @PostMapping
    public ResponseEntity<OrderDtos.OrderResponse> create(@Valid @RequestBody OrderDtos.CreateOrderRequest req) {
        OrderDtos.OrderResponse created = orderService.create(req);
        return ResponseEntity.created(URI.create("/api/orders/" + created.id())).body(created);
    }

    @Operation(summary = "List orders")
    @GetMapping
    public List<OrderDtos.OrderResponse> list() {
        return orderService.list();
    }

    @Operation(summary = "Get order by id")
    @GetMapping("/{id}")
    public OrderDtos.OrderResponse get(@PathVariable Long id) {
        return orderService.get(id);
    }

    @Operation(summary = "Update order status (PATCH)")
    @PatchMapping("/{id}")
    public OrderDtos.OrderResponse patchStatus(@PathVariable Long id, @Valid @RequestBody OrderDtos.PatchOrderRequest req) {
        return orderService.patchStatus(id, req);
    }

    @Operation(summary = "Delete order")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        orderService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
