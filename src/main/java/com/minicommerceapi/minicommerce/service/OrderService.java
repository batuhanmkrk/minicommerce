package com.minicommerceapi.minicommerce.service;

import com.minicommerceapi.minicommerce.domain.*;
import com.minicommerceapi.minicommerce.dto.OrderDtos;
import com.minicommerceapi.minicommerce.exception.BadRequestException;
import com.minicommerceapi.minicommerce.exception.ConflictException;
import com.minicommerceapi.minicommerce.exception.NotFoundException;
import com.minicommerceapi.minicommerce.repo.OrderRepository;
import com.minicommerceapi.minicommerce.repo.ProductRepository;
import com.minicommerceapi.minicommerce.repo.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    public OrderService(OrderRepository orderRepository, UserRepository userRepository, ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public OrderDtos.OrderResponse create(OrderDtos.CreateOrderRequest req) {

        // Transactional onemli: siparis olusurken hata alinirsa stok dusumunun rollback olmasi gerekir.
        User user = userRepository.findById(req.userId()).orElseThrow(() -> new NotFoundException("User not found"));

        Order order = new Order();
        order.setUser(user);
        order.setStatus(OrderStatus.CREATED);

        BigDecimal total = BigDecimal.ZERO;

        for (OrderDtos.CreateOrderItem itemReq : req.items()) {
            Product product = productRepository.findById(itemReq.productId())
                    .orElseThrow(() -> new NotFoundException("Product not found: " + itemReq.productId()));

            if (itemReq.quantity() <= 0) {
                throw new BadRequestException("Quantity must be >= 1");
            }
            if (product.getStock() < itemReq.quantity()) {
                throw new BadRequestException("Insufficient stock for product " + product.getId());
            }

            product.setStock(product.getStock() - itemReq.quantity()); // stok dusumu (basit senaryo)

            OrderItem item = new OrderItem();
            item.setProduct(product);
            item.setQuantity(itemReq.quantity());
            item.setUnitPrice(product.getPrice());
            item.setLineTotal(product.getPrice().multiply(BigDecimal.valueOf(itemReq.quantity())));

            total = total.add(item.getLineTotal());
            order.addItem(item);
        }

        order.setTotal(total);
        Order saved = orderRepository.save(order);

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<OrderDtos.OrderResponse> list() {
        return orderRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public OrderDtos.OrderResponse get(Long id) {
        Order order = orderRepository.findById(id).orElseThrow(() -> new NotFoundException("Order not found"));
        return toResponse(order);
    }

    @Transactional
    public OrderDtos.OrderResponse patchStatus(Long id, OrderDtos.PatchOrderRequest req) {
        // Basit bir durum makinasi: CREATED -> PAID veya CANCELLED. Sonrasi terminal.
        Order order = orderRepository.findById(id).orElseThrow(() -> new NotFoundException("Order not found"));

        OrderStatus newStatus;
        try {
            newStatus = OrderStatus.valueOf(req.status().trim().toUpperCase());
        } catch (Exception e) {
            throw new BadRequestException("Invalid status. Allowed: CREATED, PAID, CANCELLED");
        }

        if (order.getStatus() != OrderStatus.CREATED) {
            throw new ConflictException("Order status cannot be changed after it is " + order.getStatus());
        }
        if (newStatus == OrderStatus.CREATED) {
            throw new BadRequestException("Order is already CREATED");
        }

        order.setStatus(newStatus);
        return toResponse(order);
    }

    @Transactional
    public void delete(Long id) {
        if (!orderRepository.existsById(id)) {
            throw new NotFoundException("Order not found");
        }
        orderRepository.deleteById(id);
    }

    private OrderDtos.OrderResponse toResponse(Order o) {
        List<OrderDtos.OrderItemResponse> items = o.getItems().stream()
                .map(oi -> new OrderDtos.OrderItemResponse(
                        oi.getProduct().getId(),
                        oi.getProduct().getName(),
                        oi.getQuantity(),
                        oi.getUnitPrice(),
                        oi.getLineTotal()
                ))
                .toList();

        return new OrderDtos.OrderResponse(o.getId(), o.getUser().getId(), o.getStatus().name(), o.getTotal(), items);
    }
}
