package com.minicommerceapi.minicommerce.e2e;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;


import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest(
        classes = com.minicommerceapi.minicommerce.MinicommerceApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK
)

@AutoConfigureMockMvc
@Transactional
class MinicommerceE2ETest {

    @Autowired
    MockMvc mockMvc;

    private String uniq(String base) {
        return base + "-" + System.nanoTime();
    }

    private long idFrom(MvcResult r) throws Exception {
        String json = r.getResponse().getContentAsString();
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }

    private long createCategory(String name) throws Exception {
        MvcResult r = mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andReturn();
        return idFrom(r);
    }

    private long createUser(String name, String email) throws Exception {
        MvcResult r = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"email\":\"" + email + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andReturn();
        return idFrom(r);
    }

    private long createProduct(String name, String sku, String price, int stock, long categoryId) throws Exception {
        String body = String.format(
                "{\"name\":\"%s\",\"sku\":\"%s\",\"price\":%s,\"stock\":%d,\"categoryId\":%d}",
                name, sku, price, stock, categoryId
        );

        MvcResult r = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.sku").value(sku))
                .andReturn();
        return idFrom(r);
    }

    private long createOrder(long userId, long productId, int qty) throws Exception {
        String body = String.format(
                "{\"userId\":%d,\"items\":[{\"productId\":%d,\"quantity\":%d}]}",
                userId, productId, qty
        );
        MvcResult r = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andReturn();
        return idFrom(r);
    }

    private long createReview(long userId, long productId, int rating, String comment) throws Exception {
        String body = String.format(
                "{\"userId\":%d,\"productId\":%d,\"rating\":%d,\"comment\":\"%s\"}",
                userId, productId, rating, comment
        );
        MvcResult r = mockMvc.perform(post("/api/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andReturn();
        return idFrom(r);
    }

    // SENARYO 1: Category -> Product -> User -> Order, stok dusumu kontrolu
    @Test
    void e2e_fullFlow_shouldCreateOrderAndDecreaseStock() throws Exception {
        long categoryId = createCategory(uniq("Electronics"));
        long productId  = createProduct("Laptop", uniq("SKU"), "1000.00", 5, categoryId);
        long userId     = createUser("Ali", uniq("ali") + "@test.com");

        long orderId = createOrder(userId, productId, 2);

        mockMvc.perform(get("/api/orders/" + orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value((int) userId))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].productId").value((int) productId))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.total", notNullValue()));

        // stok 5 -> 3
        mockMvc.perform(get("/api/products/" + productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(3));
    }

    // SENARYO 2: Product CRUD
    @Test
    void e2e_productCrud_shouldWork() throws Exception {
        long categoryId = createCategory(uniq("Books"));
        long productId  = createProduct("Clean Code", uniq("BOOK"), "300.00", 10, categoryId);

        mockMvc.perform(get("/api/products/" + productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Clean Code"));

        mockMvc.perform(patch("/api/products/" + productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"price\":350.00,\"stock\":7}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(7))
                .andExpect(jsonPath("$.price", notNullValue()));

        mockMvc.perform(delete("/api/products/" + productId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/products/" + productId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Product not found"))
                .andExpect(jsonPath("$.path").value("/api/products/" + productId));
    }

    // SENARYO 3: Order status transition (CREATED -> PAID, sonra terminal -> 409)
    @Test
    void e2e_orderStatusUpdate_shouldEnforceStateRules() throws Exception {
        long categoryId = createCategory(uniq("Food"));
        long productId  = createProduct("Pizza", uniq("FOOD"), "100.00", 20, categoryId);
        long userId     = createUser("Veli", uniq("veli") + "@test.com");
        long orderId    = createOrder(userId, productId, 1);

        mockMvc.perform(patch("/api/orders/" + orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"PAID\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));

        mockMvc.perform(patch("/api/orders/" + orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CANCELLED\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("Order status cannot be changed")));
    }

    // SENARYO 4: Review flow
    @Test
    void e2e_reviewFlow_shouldWork() throws Exception {
        long categoryId = createCategory(uniq("Games"));
        long productId  = createProduct("Chess", uniq("GAME"), "200.00", 5, categoryId);
        long userId     = createUser("Ayse", uniq("ayse") + "@test.com");

        long reviewId = createReview(userId, productId, 5, "Great!");

        mockMvc.perform(patch("/api/reviews/" + reviewId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":4,\"comment\":\"Still good\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(4))
                .andExpect(jsonPath("$.comment").value("Still good"));

        mockMvc.perform(get("/api/reviews?productId=" + productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", not(empty())))
                .andExpect(jsonPath("$[0].productId").value((int) productId));

        mockMvc.perform(delete("/api/reviews/" + reviewId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/reviews/" + reviewId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Review not found"));
    }

    // SENARYO 5: Validation error (400 + violations)
    @Test
    void e2e_validation_shouldReturn400WithViolations() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"email\":\"not-an-email\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.violations[*].field", hasItems("name", "email")))
                .andExpect(jsonPath("$.path").value("/api/users"));
    }
}
