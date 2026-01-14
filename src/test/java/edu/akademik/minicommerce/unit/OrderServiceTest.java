package edu.akademik.minicommerce.unit;

import edu.akademik.minicommerce.domain.Category;
import edu.akademik.minicommerce.domain.Product;
import edu.akademik.minicommerce.domain.User;
import edu.akademik.minicommerce.dto.OrderDtos;
import edu.akademik.minicommerce.exception.BadRequestException;
import edu.akademik.minicommerce.exception.NotFoundException;
import edu.akademik.minicommerce.repo.OrderRepository;
import edu.akademik.minicommerce.repo.ProductRepository;
import edu.akademik.minicommerce.repo.UserRepository;
import edu.akademik.minicommerce.service.OrderService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OrderServiceTest {

    @Test
    void create_computesTotal_andDecreasesStock() {
        OrderRepository or = mock(OrderRepository.class);
        UserRepository ur = mock(UserRepository.class);
        ProductRepository pr = mock(ProductRepository.class);

        User u = new User(); u.setName("A"); u.setEmail("a@b.com");
        when(ur.findById(1L)).thenReturn(Optional.of(u));

        Category cat = new Category(); cat.setName("Cat"); cat.setSlug("cat");
        Product p1 = new Product(); p1.setName("P1"); p1.setSku("S1"); p1.setPrice(new BigDecimal("10.00")); p1.setStock(5); p1.setCategory(cat);

        when(pr.findById(100L)).thenReturn(Optional.of(p1));

        when(or.save(any())).thenAnswer(inv -> inv.getArgument(0)); // return same

        OrderService svc = new OrderService(or, ur, pr);
        var res = svc.create(new OrderDtos.CreateOrderRequest(1L, List.of(new OrderDtos.CreateOrderItem(100L, 2))));

        assertEquals(new BigDecimal("20.00"), res.total());
        assertEquals(3, p1.getStock());
        assertEquals(1, res.items().size());
    }

    @Test
    void create_insufficientStock_throwsBadRequest() {
        OrderRepository or = mock(OrderRepository.class);
        UserRepository ur = mock(UserRepository.class);
        ProductRepository pr = mock(ProductRepository.class);

        User u = new User(); u.setName("A"); u.setEmail("a@b.com");
        when(ur.findById(1L)).thenReturn(Optional.of(u));

        Category cat = new Category(); cat.setName("Cat"); cat.setSlug("cat");
        Product p1 = new Product(); p1.setName("P1"); p1.setSku("S1"); p1.setPrice(new BigDecimal("10.00")); p1.setStock(1); p1.setCategory(cat);
        when(pr.findById(100L)).thenReturn(Optional.of(p1));

        OrderService svc = new OrderService(or, ur, pr);
        assertThrows(BadRequestException.class, () -> svc.create(new OrderDtos.CreateOrderRequest(1L, List.of(
                new OrderDtos.CreateOrderItem(100L, 2)
        ))));
    }

    @Test
    void create_userMissing_throwsNotFound() {
        OrderRepository or = mock(OrderRepository.class);
        UserRepository ur = mock(UserRepository.class);
        ProductRepository pr = mock(ProductRepository.class);

        when(ur.findById(1L)).thenReturn(Optional.empty());

        OrderService svc = new OrderService(or, ur, pr);
        assertThrows(NotFoundException.class, () -> svc.create(new OrderDtos.CreateOrderRequest(1L, List.of(
                new OrderDtos.CreateOrderItem(100L, 1)
        ))));
    }

    @Test
    void create_productMissing_throwsNotFound() {
        OrderRepository or = mock(OrderRepository.class);
        UserRepository ur = mock(UserRepository.class);
        ProductRepository pr = mock(ProductRepository.class);

        User u = new User(); u.setName("A"); u.setEmail("a@b.com");
        when(ur.findById(1L)).thenReturn(Optional.of(u));
        when(pr.findById(100L)).thenReturn(Optional.empty());

        OrderService svc = new OrderService(or, ur, pr);
        assertThrows(NotFoundException.class, () -> svc.create(new OrderDtos.CreateOrderRequest(1L, List.of(
                new OrderDtos.CreateOrderItem(100L, 1)
        ))));
    }
}
