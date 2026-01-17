package com.minicommerceapi.minicommerce.e2e;

import com.minicommerceapi.minicommerce.MinicommerceApplication;
import com.minicommerceapi.minicommerce.dto.*;
import com.minicommerceapi.minicommerce.exception.BadRequestException;
import com.minicommerceapi.minicommerce.exception.ConflictException;
import com.minicommerceapi.minicommerce.exception.NotFoundException;
import com.minicommerceapi.minicommerce.repo.ProductRepository;
import com.minicommerceapi.minicommerce.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = MinicommerceApplication.class)
@TestPropertySource(properties = {
        // Prod sqlite dosyasını kilitlemesin diye test sırasında ayrı DB kullanıyoruz
        "spring.datasource.url=jdbc:sqlite:./target/test-e2e.db",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class MinicommerceServiceE2ETest {

    @Autowired CategoryService categoryService;
    @Autowired ProductService productService;
    @Autowired UserService userService;
    @Autowired OrderService orderService;
    @Autowired ReviewService reviewService;

    @Autowired ProductRepository productRepository;

    private final TransactionTemplate tx;

    MinicommerceServiceE2ETest(@Autowired PlatformTransactionManager tm) {
        this.tx = new TransactionTemplate(tm);
    }

    private String uniq(String base) {
        return base + "-" + System.nanoTime();
    }

    /**
     * SENARYO 1:
     * Category -> Product -> User -> Order (stok düşümü + total kontrol)
     */
    @Test
    void e2e_fullFlow_shouldCreateOrderAndDecreaseStock() {
        var cat = categoryService.create(new CategoryDtos.CreateCategoryRequest(uniq("Electronics")));
        var prod = productService.create(new ProductDtos.CreateProductRequest(
                "Laptop", uniq("SKU"), new BigDecimal("1000.00"), 5, cat.id()
        ));
        var user = userService.create(new UserDtos.CreateUserRequest("Ali", uniq("ali") + "@test.com"));

        var order = orderService.create(new OrderDtos.CreateOrderRequest(
                user.id(),
                List.of(new OrderDtos.CreateOrderItem(prod.id(), 2))
        ));

        assertNotNull(order.id());
        assertEquals(user.id(), order.userId());
        assertEquals("CREATED", order.status());
        assertEquals(1, order.items().size());
        assertEquals(prod.id(), order.items().get(0).productId());
        assertEquals(2, order.items().get(0).quantity());
        assertEquals(new BigDecimal("2000.00"), order.total());

        var prodAfter = productService.get(prod.id());
        assertEquals(3, prodAfter.stock()); // 5 -> 3
    }

    /**
     * SENARYO 2:
     * Product CRUD (create -> get -> patch -> delete -> get throws 404)
     */
    @Test
    void e2e_productCrud_shouldWork() {
        var cat = categoryService.create(new CategoryDtos.CreateCategoryRequest(uniq("Books")));
        var p = productService.create(new ProductDtos.CreateProductRequest(
                "Clean Code", uniq("BOOK"), new BigDecimal("300.00"), 10, cat.id()
        ));

        var fetched = productService.get(p.id());
        assertEquals("Clean Code", fetched.name());

        var patched = productService.patch(p.id(), new ProductDtos.PatchProductRequest(
                null, null, new BigDecimal("350.00"), 7, null
        ));
        assertEquals(new BigDecimal("350.00"), patched.price());
        assertEquals(7, patched.stock());

        productService.delete(p.id());
        assertThrows(NotFoundException.class, () -> productService.get(p.id()));
    }

    /**
     * SENARYO 3:
     * Order status state machine (CREATED -> PAID, sonra değişemez -> 409)
     */
    @Test
    void e2e_orderStatus_shouldEnforceRules() {
        var cat = categoryService.create(new CategoryDtos.CreateCategoryRequest(uniq("Food")));
        var prod = productService.create(new ProductDtos.CreateProductRequest(
                "Pizza", uniq("FOOD"), new BigDecimal("100.00"), 20, cat.id()
        ));
        var user = userService.create(new UserDtos.CreateUserRequest("Veli", uniq("veli") + "@test.com"));

        var order = orderService.create(new OrderDtos.CreateOrderRequest(
                user.id(),
                List.of(new OrderDtos.CreateOrderItem(prod.id(), 1))
        ));
        assertEquals("CREATED", order.status());

        var paid = orderService.patchStatus(order.id(), new OrderDtos.PatchOrderRequest("PAID"));
        assertEquals("PAID", paid.status());

        assertThrows(ConflictException.class,
                () -> orderService.patchStatus(order.id(), new OrderDtos.PatchOrderRequest("CANCELLED")));
    }

    /**
     * SENARYO 4:
     * Review flow (create -> patch -> list(filter) -> delete)
     */
    @Test
    void e2e_reviewFlow_shouldWork() {
        var cat = categoryService.create(new CategoryDtos.CreateCategoryRequest(uniq("Games")));
        var prod = productService.create(new ProductDtos.CreateProductRequest(
                "Chess", uniq("GAME"), new BigDecimal("200.00"), 5, cat.id()
        ));
        var user = userService.create(new UserDtos.CreateUserRequest("Ayse", uniq("ayse") + "@test.com"));

        var r = reviewService.create(new ReviewDtos.CreateReviewRequest(
                user.id(), prod.id(), 5, "Great!"
        ));
        assertNotNull(r.id());
        assertEquals(5, r.rating());

        var patched = reviewService.patch(r.id(), new ReviewDtos.PatchReviewRequest(4, "Still good"));
        assertEquals(4, patched.rating());
        assertEquals("Still good", patched.comment());

        var list = reviewService.list(prod.id());
        assertFalse(list.isEmpty());
        assertTrue(list.stream().allMatch(x -> x.productId().equals(prod.id())));

        reviewService.delete(r.id());
        assertThrows(NotFoundException.class, () -> reviewService.get(r.id()));
    }

    /**
     * SENARYO 5:
     * Transaction rollback kanıtı:
     * Order içinde A ürünü stok düşürülüyor, B ürünü stok yetersiz -> hata.
     * Beklenen: tüm işlem rollback, A'nın stoğu eski haline dönmüş olmalı.
     */
    @Test
    void e2e_orderCreate_shouldRollbackStockOnFailure() {
        var cat = categoryService.create(new CategoryDtos.CreateCategoryRequest(uniq("RollbackCat")));
        var user = userService.create(new UserDtos.CreateUserRequest("Can", uniq("can") + "@test.com"));

        var a = productService.create(new ProductDtos.CreateProductRequest(
                "A", uniq("A-SKU"), new BigDecimal("10.00"), 5, cat.id()
        ));
        var b = productService.create(new ProductDtos.CreateProductRequest(
                "B", uniq("B-SKU"), new BigDecimal("10.00"), 1, cat.id()
        ));

        // Bu createOrder bir transaction içinde koşacak, exception ile rollback olacak.
        assertThrows(BadRequestException.class, () -> tx.execute(status -> {
            orderService.create(new OrderDtos.CreateOrderRequest(
                    user.id(),
                    List.of(
                            new OrderDtos.CreateOrderItem(a.id(), 2), // burada A stoğu 5->3 düşer
                            new OrderDtos.CreateOrderItem(b.id(), 2)  // burada yetersiz stok -> exception
                    )
            ));
            return null;
        }));

        // Yeni bir transaction içinde DB'den tekrar okuyup stoğun geri döndüğünü doğrula
        Integer stockA = tx.execute(status ->
                productRepository.findById(a.id()).orElseThrow().getStock()
        );

        assertEquals(5, stockA); // rollback olduysa 5 kalmalı
    }
}
