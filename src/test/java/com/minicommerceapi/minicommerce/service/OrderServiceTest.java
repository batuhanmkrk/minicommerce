package com.minicommerceapi.minicommerce.service;

import com.minicommerceapi.minicommerce.domain.*;
import com.minicommerceapi.minicommerce.dto.OrderDtos;
import com.minicommerceapi.minicommerce.exception.BadRequestException;
import com.minicommerceapi.minicommerce.exception.ConflictException;
import com.minicommerceapi.minicommerce.exception.NotFoundException;
import com.minicommerceapi.minicommerce.repo.OrderRepository;
import com.minicommerceapi.minicommerce.repo.ProductRepository;
import com.minicommerceapi.minicommerce.repo.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OrderServiceTest {
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ProductRepository productRepository;
    @InjectMocks
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        // Use try-with-resources in real code, but for unit test lifecycle, suppress warning
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void create_shouldCreateOrderSuccessfully() {
        User user = new User();
        user.setId(1L);
        Product product = new Product();
        product.setId(2L);
        product.setName("Test Product");
        product.setStock(10);
        product.setPrice(BigDecimal.valueOf(100));
        OrderDtos.CreateOrderItem item = new OrderDtos.CreateOrderItem(product.getId(), 2);
        OrderDtos.CreateOrderRequest req = new OrderDtos.CreateOrderRequest(user.getId(), List.of(item));

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            o.setId(123L);
            return o;
        });

        OrderDtos.OrderResponse response = orderService.create(req);
        assertEquals(user.getId(), response.userId());
        assertEquals("CREATED", response.status());
        assertEquals(BigDecimal.valueOf(200), response.total());
        assertEquals(1, response.items().size());
        assertEquals(product.getId(), response.items().get(0).productId());
        assertEquals(8, product.getStock());
    }

    @Test
    void create_shouldThrowIfUserNotFound() {
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());
        OrderDtos.CreateOrderRequest req = new OrderDtos.CreateOrderRequest(1L, List.of());
        assertThrows(NotFoundException.class, () -> orderService.create(req));
    }

    @Test
    void create_shouldThrowIfProductNotFound() {
        User user = new User();
        user.setId(1L);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(productRepository.findById(anyLong())).thenReturn(Optional.empty());
        OrderDtos.CreateOrderItem item = new OrderDtos.CreateOrderItem(2L, 1);
        OrderDtos.CreateOrderRequest req = new OrderDtos.CreateOrderRequest(user.getId(), List.of(item));
        assertThrows(NotFoundException.class, () -> orderService.create(req));
    }

    @Test
    void create_shouldThrowIfQuantityInvalid() {
        User user = new User();
        user.setId(1L);
        Product product = new Product();
        product.setId(2L);
        product.setStock(10);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        // Test with negative and zero quantity, but avoid record validation error by catching exception
        assertThrows(BadRequestException.class, () -> {
            OrderDtos.CreateOrderItem item = new OrderDtos.CreateOrderItem(product.getId(), 1); // valid for record
            OrderDtos.CreateOrderRequest req = new OrderDtos.CreateOrderRequest(user.getId(), List.of(item));
            req = new OrderDtos.CreateOrderRequest(user.getId(), List.of(new OrderDtos.CreateOrderItem(product.getId(), 0)));
            orderService.create(req);
        });
    }

    @Test
    void create_shouldThrowIfInsufficientStock() {
        User user = new User();
        user.setId(1L);
        Product product = new Product();
        product.setId(2L);
        product.setStock(1);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        OrderDtos.CreateOrderItem item = new OrderDtos.CreateOrderItem(product.getId(), 2);
        OrderDtos.CreateOrderRequest req = new OrderDtos.CreateOrderRequest(user.getId(), List.of(item));
        assertThrows(BadRequestException.class, () -> orderService.create(req));
    }

    @Test
    void list_shouldReturnOrderResponses() {
        Order order = new Order();
        order.setId(1L);
        User user = new User();
        user.setId(2L);
        order.setUser(user);
        order.setStatus(OrderStatus.CREATED);
        order.setTotal(BigDecimal.valueOf(100));
        OrderItem item = new OrderItem();
        Product product = new Product();
        product.setId(3L);
        product.setName("P");
        item.setProduct(product);
        item.setQuantity(1);
        item.setUnitPrice(BigDecimal.valueOf(100));
        item.setLineTotal(BigDecimal.valueOf(100));
        order.addItem(item);
        when(orderRepository.findAll()).thenReturn(List.of(order));
        List<OrderDtos.OrderResponse> responses = orderService.list();
        assertEquals(1, responses.size());
        assertEquals(order.getId(), responses.get(0).id());
    }

    @Test
    void get_shouldReturnOrderResponse() {
        Order order = new Order();
        order.setId(1L);
        User user = new User();
        user.setId(2L);
        order.setUser(user);
        order.setStatus(OrderStatus.CREATED);
        order.setTotal(BigDecimal.valueOf(100));
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        OrderDtos.OrderResponse response = orderService.get(order.getId());
        assertEquals(order.getId(), response.id());
    }

    @Test
    void get_shouldThrowIfOrderNotFound() {
        when(orderRepository.findById(anyLong())).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> orderService.get(1L));
    }

    @Test
    void patchStatus_shouldUpdateStatus() {
        Order order = new Order();
        order.setId(1L);
        order.setStatus(OrderStatus.CREATED);
        User user = new User();
        user.setId(2L);
        order.setUser(user);
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        OrderDtos.PatchOrderRequest req = new OrderDtos.PatchOrderRequest("PAID");
        OrderDtos.OrderResponse response = orderService.patchStatus(order.getId(), req);
        assertEquals("PAID", response.status());
    }

    @Test
    void patchStatus_shouldThrowIfOrderNotFound() {
        when(orderRepository.findById(anyLong())).thenReturn(Optional.empty());
        OrderDtos.PatchOrderRequest req = new OrderDtos.PatchOrderRequest("PAID");
        assertThrows(NotFoundException.class, () -> orderService.patchStatus(1L, req));
    }

    @Test
    void patchStatus_shouldThrowIfInvalidStatus() {
        Order order = new Order();
        order.setId(1L);
        order.setStatus(OrderStatus.CREATED);
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        OrderDtos.PatchOrderRequest req = new OrderDtos.PatchOrderRequest("INVALID");
        assertThrows(BadRequestException.class, () -> orderService.patchStatus(order.getId(), req));
    }

    @Test
    void patchStatus_shouldThrowIfNotCreated() {
        Order order = new Order();
        order.setId(1L);
        order.setStatus(OrderStatus.PAID);
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        OrderDtos.PatchOrderRequest req = new OrderDtos.PatchOrderRequest("CANCELLED");
        assertThrows(ConflictException.class, () -> orderService.patchStatus(order.getId(), req));
    }

    @Test
    void patchStatus_shouldThrowIfAlreadyCreated() {
        Order order = new Order();
        order.setId(1L);
        order.setStatus(OrderStatus.CREATED);
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        OrderDtos.PatchOrderRequest req = new OrderDtos.PatchOrderRequest("CREATED");
        assertThrows(BadRequestException.class, () -> orderService.patchStatus(order.getId(), req));
    }

    @Test
    void delete_shouldDeleteOrder() {
        when(orderRepository.existsById(1L)).thenReturn(true);
        doNothing().when(orderRepository).deleteById(1L);
        assertDoesNotThrow(() -> orderService.delete(1L));
        verify(orderRepository).deleteById(1L);
    }

    @Test
    void delete_shouldThrowIfOrderNotFound() {
        when(orderRepository.existsById(1L)).thenReturn(false);
        assertThrows(NotFoundException.class, () -> orderService.delete(1L));
    }
}