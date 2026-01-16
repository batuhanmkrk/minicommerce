package com.minicommerceapi.e2e;

import com.minicommerceapi.dto.CategoryDtos;
import com.minicommerceapi.dto.OrderDtos;
import com.minicommerceapi.dto.ProductDtos;
import com.minicommerceapi.dto.ReviewDtos;
import com.minicommerceapi.dto.UserDtos;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApiE2ETest {

    @Autowired
    TestRestTemplate rest;

    @Test
    void scenario_userLifecycle_create_get_delete() {
        var created = rest.postForObject("/api/users",
                new UserDtos.CreateUserRequest("Mehmet", "mehmet@ornek.com"),
                UserDtos.UserResponse.class);

        assertNotNull(created);
        assertNotNull(created.id());

        var fetched = rest.getForObject("/api/users/" + created.id(), UserDtos.UserResponse.class);
        assertEquals("Mehmet", fetched.name());

        rest.delete("/api/users/" + created.id());

        ResponseEntity<String> afterDelete = rest.getForEntity("/api/users/" + created.id(), String.class);
        assertEquals(HttpStatus.NOT_FOUND, afterDelete.getStatusCode());
    }

    @Test
    void scenario_categoryDelete_withProducts_returnsConflict() {
        long catId = rest.postForObject("/api/categories",
                new CategoryDtos.CreateCategoryRequest("Elektronik"),
                CategoryDtos.CategoryResponse.class).id();

        rest.postForObject("/api/products",
                new ProductDtos.CreateProductRequest("Kulaklik", "SKU-TR-99", new BigDecimal("250.00"), 5, catId),
                ProductDtos.ProductResponse.class);

        ResponseEntity<String> del = rest.exchange("/api/categories/" + catId, HttpMethod.DELETE, null, String.class);
        assertEquals(HttpStatus.CONFLICT, del.getStatusCode());
    }

    @Test
    void scenario_orderCreation_decreasesStock() {
        long userId = rest.postForObject("/api/users",
                new UserDtos.CreateUserRequest("Zeynep", "zeynep@ornek.com"),
                UserDtos.UserResponse.class).id();

        long catId = rest.postForObject("/api/categories",
                new CategoryDtos.CreateCategoryRequest("Ofis"),
                CategoryDtos.CategoryResponse.class).id();

        long productId = rest.postForObject("/api/products",
                new ProductDtos.CreateProductRequest("Defter", "SKU-TR-10", new BigDecimal("30.00"), 10, catId),
                ProductDtos.ProductResponse.class).id();

        var before = rest.getForObject("/api/products/" + productId, ProductDtos.ProductResponse.class);
        assertEquals(10, before.stock());

        var order = rest.postForObject("/api/orders",
                new OrderDtos.CreateOrderRequest(userId, List.of(new OrderDtos.CreateOrderItem(productId, 2))),
                OrderDtos.OrderResponse.class);

        assertNotNull(order);
        assertEquals(1, order.items().size());

        var after = rest.getForObject("/api/products/" + productId, ProductDtos.ProductResponse.class);
        assertEquals(8, after.stock());
    }

    @Test
    void scenario_reviewPatch_updatesRatingAndComment() {
        long userId = rest.postForObject("/api/users",
                new UserDtos.CreateUserRequest("Ahmet", "ahmet@ornek.com"),
                UserDtos.UserResponse.class).id();

        long catId = rest.postForObject("/api/categories",
                new CategoryDtos.CreateCategoryRequest("Ev"),
                CategoryDtos.CategoryResponse.class).id();

        long productId = rest.postForObject("/api/products",
                new ProductDtos.CreateProductRequest("Masa Lambasi", "SKU-TR-55", new BigDecimal("120.00"), 3, catId),
                ProductDtos.ProductResponse.class).id();

        long reviewId = rest.postForObject("/api/reviews",
                new ReviewDtos.CreateReviewRequest(userId, productId, 3, "idare eder"),
                ReviewDtos.ReviewResponse.class).id();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> patchReq = new HttpEntity<>("{\"rating\":5,\"comment\":\"cok iyi\"}", headers);

        ResponseEntity<ReviewDtos.ReviewResponse> patched = rest.exchange("/api/reviews/" + reviewId,
                HttpMethod.PATCH, patchReq, ReviewDtos.ReviewResponse.class);

        assertEquals(HttpStatus.OK, patched.getStatusCode());
        assertNotNull(patched.getBody());
        assertEquals(5, patched.getBody().rating());
        assertEquals("cok iyi", patched.getBody().comment());
    }

    @Test
    void scenario_updateOrderStatus_createdToPaid() {
        long userId = rest.postForObject("/api/users",
                new UserDtos.CreateUserRequest("Elif", "elif@ornek.com"),
                UserDtos.UserResponse.class).id();

        long catId = rest.postForObject("/api/categories",
                new CategoryDtos.CreateCategoryRequest("Kirtasiye"),
                CategoryDtos.CategoryResponse.class).id();

        long productId = rest.postForObject("/api/products",
                new ProductDtos.CreateProductRequest("Kalem", "SKU-TR-77", new BigDecimal("12.00"), 20, catId),
                ProductDtos.ProductResponse.class).id();

        var order = rest.postForObject("/api/orders",
                new OrderDtos.CreateOrderRequest(userId, List.of(new OrderDtos.CreateOrderItem(productId, 1))),
                OrderDtos.OrderResponse.class);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> patchReq = new HttpEntity<>("{\"status\":\"PAID\"}", headers);

        ResponseEntity<OrderDtos.OrderResponse> patched = rest.exchange("/api/orders/" + order.id(),
                HttpMethod.PATCH, patchReq, OrderDtos.OrderResponse.class);

        assertEquals(HttpStatus.OK, patched.getStatusCode());
        assertNotNull(patched.getBody());
        assertEquals("PAID", patched.getBody().status());
    }

    @Test
    void scenario_validationError_returns400() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> req = new HttpEntity<>("{\"name\":\"\",\"email\":\"yanlis\"}", headers);

        ResponseEntity<Map> res = rest.postForEntity("/api/users", req, Map.class);
        assertEquals(HttpStatus.BAD_REQUEST, res.getStatusCode());
    }
}
