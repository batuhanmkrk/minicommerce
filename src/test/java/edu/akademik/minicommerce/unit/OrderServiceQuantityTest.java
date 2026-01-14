package edu.akademik.minicommerce.unit;

import edu.akademik.minicommerce.domain.Category;
import edu.akademik.minicommerce.domain.Product;
import edu.akademik.minicommerce.domain.User;
import edu.akademik.minicommerce.dto.OrderDtos;
import edu.akademik.minicommerce.exception.BadRequestException;
import edu.akademik.minicommerce.repo.OrderRepository;
import edu.akademik.minicommerce.repo.ProductRepository;
import edu.akademik.minicommerce.repo.UserRepository;
import edu.akademik.minicommerce.service.OrderService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class OrderServiceQuantityTest {

    @Test
    void create_quantityZero_throwsBadRequest() {
        OrderRepository or = mock(OrderRepository.class);
        UserRepository ur = mock(UserRepository.class);
        ProductRepository pr = mock(ProductRepository.class);

        User u = new User(); u.setName("A"); u.setEmail("a@b.com");
        when(ur.findById(1L)).thenReturn(Optional.of(u));

        Category cat = new Category(); cat.setName("Cat"); cat.setSlug("cat");
        Product p1 = new Product(); p1.setName("P1"); p1.setSku("S1"); p1.setPrice(new BigDecimal("10.00")); p1.setStock(5); p1.setCategory(cat);
        when(pr.findById(100L)).thenReturn(Optional.of(p1));

        OrderService svc = new OrderService(or, ur, pr);
        assertThrows(BadRequestException.class, () -> svc.create(new OrderDtos.CreateOrderRequest(1L, List.of(
                new OrderDtos.CreateOrderItem(100L, 0)
        ))));
    }
}
