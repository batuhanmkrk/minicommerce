package edu.akademik.minicommerce.e2e;

import edu.akademik.minicommerce.dto.CategoryDtos;
import edu.akademik.minicommerce.dto.OrderDtos;
import edu.akademik.minicommerce.dto.ProductDtos;
import edu.akademik.minicommerce.dto.ReviewDtos;
import edu.akademik.minicommerce.dto.UserDtos;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApiE2ETest {

    @Autowired TestRestTemplate rest;

    @Test
    void scenario_createOrderFlow() {
        long userId = rest.postForObject("/api/users",
                new UserDtos.CreateUserRequest("Dan", "dan@example.com"),
                UserDtos.UserResponse.class).id();

        long catId = rest.postForObject("/api/categories",
                new CategoryDtos.CreateCategoryRequest("Accessories"),
                CategoryDtos.CategoryResponse.class).id();

        long productId = rest.postForObject("/api/products",
                new ProductDtos.CreateProductRequest("Cable", "SKU-C", new BigDecimal("3.00"), 10, catId),
                ProductDtos.ProductResponse.class).id();

        var order = rest.postForObject("/api/orders",
                new OrderDtos.CreateOrderRequest(userId, List.of(new OrderDtos.CreateOrderItem(productId, 3))),
                OrderDtos.OrderResponse.class);

        assertNotNull(order.id());
        assertEquals(new BigDecimal("9.00"), order.total());
    }

    @Test
    void scenario_patchThenReadBack() {
        long catId = rest.postForObject("/api/categories",
                new CategoryDtos.CreateCategoryRequest("Electronics"),
                CategoryDtos.CategoryResponse.class).id();

        var p = rest.postForObject("/api/products",
                new ProductDtos.CreateProductRequest("Keyboard", "SKU-K", new BigDecimal("20.00"), 5, catId),
                ProductDtos.ProductResponse.class);

        var patch = new ProductDtos.PatchProductRequest(null, null, new BigDecimal("18.00"), 7, null);
        ResponseEntity<ProductDtos.ProductResponse> patched = rest.exchange("/api/products/" + p.id(),
                HttpMethod.PATCH, new HttpEntity<>(patch), ProductDtos.ProductResponse.class);

        assertEquals(HttpStatus.OK, patched.getStatusCode());
        assertEquals(new BigDecimal("18.00"), patched.getBody().price());

        ProductDtos.ProductResponse again = rest.getForObject("/api/products/" + p.id(), ProductDtos.ProductResponse.class);
        assertEquals(7, again.stock());
    }

    @Test
    void scenario_validationErrorShape() {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> req = new HttpEntity<>("{\"name\":\"\",\"email\":\"bad\"}", h);

        ResponseEntity<Map> res = rest.postForEntity("/api/users", req, Map.class);
        assertEquals(HttpStatus.BAD_REQUEST, res.getStatusCode());
        assertTrue(res.getBody().containsKey("violations"));
    }

    @Test
    void scenario_reviewFlow() {
        long userId = rest.postForObject("/api/users",
                new UserDtos.CreateUserRequest("Eve", "eve@example.com"),
                UserDtos.UserResponse.class).id();

        long catId = rest.postForObject("/api/categories",
                new CategoryDtos.CreateCategoryRequest("Stationery"),
                CategoryDtos.CategoryResponse.class).id();

        long productId = rest.postForObject("/api/products",
                new ProductDtos.CreateProductRequest("Pen", "SKU-PEN", new BigDecimal("1.50"), 100, catId),
                ProductDtos.ProductResponse.class).id();

        var review = rest.postForObject("/api/reviews",
                new ReviewDtos.CreateReviewRequest(userId, productId, 5, "Nice pen"),
                ReviewDtos.ReviewResponse.class);

        assertNotNull(review.id());

        ResponseEntity<ReviewDtos.ReviewResponse> get = rest.getForEntity("/api/reviews/" + review.id(), ReviewDtos.ReviewResponse.class);
        assertEquals(HttpStatus.OK, get.getStatusCode());
        assertEquals(5, get.getBody().rating());
    }

    @Test
    void scenario_deleteThenNotFound() {
        long userId = rest.postForObject("/api/users",
                new UserDtos.CreateUserRequest("Frank", "frank@example.com"),
                UserDtos.UserResponse.class).id();

        rest.delete("/api/users/" + userId);

        ResponseEntity<String> after = rest.getForEntity("/api/users/" + userId, String.class);
        assertEquals(HttpStatus.NOT_FOUND, after.getStatusCode());
    }

@Test
void scenario_updateOrderStatus() {
    long userId = rest.postForObject("/api/users",
            new UserDtos.CreateUserRequest("Gina", "gina@example.com"),
            UserDtos.UserResponse.class).id();

    long catId = rest.postForObject("/api/categories",
            new CategoryDtos.CreateCategoryRequest("Office"),
            CategoryDtos.CategoryResponse.class).id();

    long productId = rest.postForObject("/api/products",
            new ProductDtos.CreateProductRequest("Stapler", "SKU-S", new BigDecimal("4.00"), 10, catId),
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
    assertEquals("PAID", patched.getBody().status());
}
}
