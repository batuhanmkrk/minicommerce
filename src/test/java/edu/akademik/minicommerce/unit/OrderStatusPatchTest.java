package edu.akademik.minicommerce.unit;

import edu.akademik.minicommerce.domain.Order;
import edu.akademik.minicommerce.domain.OrderStatus;
import edu.akademik.minicommerce.dto.OrderDtos;
import edu.akademik.minicommerce.exception.ConflictException;
import edu.akademik.minicommerce.repo.OrderRepository;
import edu.akademik.minicommerce.repo.ProductRepository;
import edu.akademik.minicommerce.repo.UserRepository;
import edu.akademik.minicommerce.service.OrderService;
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
