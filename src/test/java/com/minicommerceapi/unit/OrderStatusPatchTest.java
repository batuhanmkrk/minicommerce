package com.minicommerceapi.unit;

import com.minicommerceapi.domain.Order;
import com.minicommerceapi.domain.OrderStatus;
import com.minicommerceapi.dto.OrderDtos;
import com.minicommerceapi.exception.ConflictException;
import com.minicommerceapi.repo.OrderRepository;
import com.minicommerceapi.repo.ProductRepository;
import com.minicommerceapi.repo.UserRepository;
import com.minicommerceapi.service.OrderService;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrderStatusPatchTest {

    @Test
    void patchStatus_terminalOrder_throwsConflict() {
        OrderRepository or = mock(OrderRepository.class);
        UserRepository ur = mock(UserRepository.class);
        ProductRepository pr = mock(ProductRepository.class);

        Order o = new Order();
        o.setStatus(OrderStatus.PAID);
        when(or.findById(1L)).thenReturn(Optional.of(o));

        OrderService svc = new OrderService(or, ur, pr);
        assertThrows(ConflictException.class, () -> svc.patchStatus(1L, new OrderDtos.PatchOrderRequest("CANCELLED")));
    }
}
