package edu.akademik.minicommerce.service;

import edu.akademik.minicommerce.domain.*;
import edu.akademik.minicommerce.dto.OrderDtos;
import edu.akademik.minicommerce.exception.BadRequestException;
import edu.akademik.minicommerce.exception.ConflictException;
import edu.akademik.minicommerce.exception.NotFoundException;
import edu.akademik.minicommerce.repo.OrderRepository;
import edu.akademik.minicommerce.repo.ProductRepository;
import edu.akademik.minicommerce.repo.UserRepository;
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

            product.setStock(product.getStock() - itemReq.quantity()); // stock decrease

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
    Order order = orderRepository.findById(id).orElseThrow(() -> new NotFoundException("Order not found"));

    OrderStatus newStatus;
    try {
        newStatus = OrderStatus.valueOf(req.status().trim().toUpperCase());
    } catch (Exception e) {
        throw new BadRequestException("Invalid status. Allowed: CREATED, PAID, CANCELLED");
    }

    // Transition rule:
    // CREATED -> PAID or CANCELLED
    // PAID/CANCELLED are terminal
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
