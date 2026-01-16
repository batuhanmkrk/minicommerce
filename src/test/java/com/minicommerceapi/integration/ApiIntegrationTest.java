package com.minicommerceapi.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minicommerceapi.dto.CategoryDtos;
import com.minicommerceapi.dto.OrderDtos;
import com.minicommerceapi.dto.ProductDtos;
import com.minicommerceapi.dto.UserDtos;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;

    @Test
    void userCrud_happyPath() throws Exception {
        // POST user
        var created = mvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(new UserDtos.CreateUserRequest("Alice", "alice@example.com"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andReturn();

        long userId = om.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        // GET user
        mvc.perform(get("/api/users/" + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is("alice@example.com")));

        // PUT user
        mvc.perform(put("/api/users/" + userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(new UserDtos.UpdateUserRequest("Alice B", "aliceb@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Alice B")));

        // DELETE user
        mvc.perform(delete("/api/users/" + userId))
                .andExpect(status().isNoContent());

        // GET after delete -> 404
        mvc.perform(get("/api/users/" + userId))
                .andExpect(status().isNotFound());
    }

    @Test
    void productCrud_andCategoryDeletionRestricted() throws Exception {
        // category
        var catCreated = mvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(new CategoryDtos.CreateCategoryRequest("Clothing"))))
                .andExpect(status().isCreated())
                .andReturn();
        long catId = om.readTree(catCreated.getResponse().getContentAsString()).get("id").asLong();

        // product
        var prodCreated = mvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(new ProductDtos.CreateProductRequest(
                                "Jacket", "SKU-1", new BigDecimal("99.90"), 10, catId
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.categoryId", is((int)catId)))
                .andReturn();
        long prodId = om.readTree(prodCreated.getResponse().getContentAsString()).get("id").asLong();

        // patch product
        mvc.perform(patch("/api/products/" + prodId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(new ProductDtos.PatchProductRequest(
                                null, null, new BigDecimal("89.90"), 8, null
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price", is(89.90)))
                .andExpect(jsonPath("$.stock", is(8)));

        // delete category should be 409 because it has products
        mvc.perform(delete("/api/categories/" + catId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("Category has products")));

        // delete product
        mvc.perform(delete("/api/products/" + prodId))
                .andExpect(status().isNoContent());

        // now delete category ok
        mvc.perform(delete("/api/categories/" + catId))
                .andExpect(status().isNoContent());
    }

    @Test
    void orderCreate_decreasesStock_andReturnsTotal() throws Exception {
        // user
        long userId = om.readTree(mvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(new UserDtos.CreateUserRequest("Bob", "bob@example.com"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).get("id").asLong();

        // category
        long catId = om.readTree(mvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(new CategoryDtos.CreateCategoryRequest("Gadgets"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).get("id").asLong();

        // product
        long prodId = om.readTree(mvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(new ProductDtos.CreateProductRequest(
                                "Mouse", "SKU-M", new BigDecimal("10.00"), 3, catId
                        ))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).get("id").asLong();

        // order
        var orderReq = new OrderDtos.CreateOrderRequest(userId, List.of(new OrderDtos.CreateOrderItem(prodId, 2)));
        mvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(orderReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.total", is(20.00)))
                .andExpect(jsonPath("$.items", hasSize(1)));

        // product stock should be 1 now (GET)
        mvc.perform(get("/api/products/" + prodId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock", is(1)));
    }

    @Test
    void orderCreate_insufficientStock_returns400() throws Exception {
        long userId = om.readTree(mvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(new UserDtos.CreateUserRequest("Carol", "carol@example.com"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).get("id").asLong();

        long catId = om.readTree(mvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(new CategoryDtos.CreateCategoryRequest("Books"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).get("id").asLong();

        long prodId = om.readTree(mvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(new ProductDtos.CreateProductRequest(
                                "Book", "SKU-B", new BigDecimal("15.00"), 1, catId
                        ))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).get("id").asLong();

        var orderReq = new OrderDtos.CreateOrderRequest(userId, List.of(new OrderDtos.CreateOrderItem(prodId, 2)));
        mvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(orderReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Insufficient stock")));
    }

    @Test
    void validation_errors_return400_withViolations() throws Exception {
        mvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"email\":\"not-an-email\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.violations", notNullValue()))
                .andExpect(jsonPath("$.violations", hasSize(greaterThanOrEqualTo(1))));
    }

@Test
void orderStatus_patch_changesStatus_fromCreatedToPaid() throws Exception {
    long userId = om.readTree(mvc.perform(post("/api/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsString(new UserDtos.CreateUserRequest("Zed", "zed@example.com"))))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString()).get("id").asLong();

    long catId = om.readTree(mvc.perform(post("/api/categories")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsString(new CategoryDtos.CreateCategoryRequest("Tools"))))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString()).get("id").asLong();

    long prodId = om.readTree(mvc.perform(post("/api/products")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsString(new ProductDtos.CreateProductRequest(
                            "Hammer", "SKU-H", new BigDecimal("8.00"), 10, catId
                    ))))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString()).get("id").asLong();

    long orderId = om.readTree(mvc.perform(post("/api/orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsString(new OrderDtos.CreateOrderRequest(
                            userId, List.of(new OrderDtos.CreateOrderItem(prodId, 1))
                    ))))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString()).get("id").asLong();

    mvc.perform(patch("/api/orders/" + orderId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"status\":\"PAID\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status", is("PAID")));
}

@Test
void review_patch_updatesRatingAndComment() throws Exception {
    long userId = om.readTree(mvc.perform(post("/api/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsString(new UserDtos.CreateUserRequest("Ivy", "ivy@example.com"))))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString()).get("id").asLong();

    long catId = om.readTree(mvc.perform(post("/api/categories")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsString(new CategoryDtos.CreateCategoryRequest("Home"))))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString()).get("id").asLong();

    long prodId = om.readTree(mvc.perform(post("/api/products")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsString(new ProductDtos.CreateProductRequest(
                            "Lamp", "SKU-L", new BigDecimal("12.00"), 5, catId
                    ))))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString()).get("id").asLong();

    long reviewId = om.readTree(mvc.perform(post("/api/reviews")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsString(new com.minicommerceapi.dto.ReviewDtos.CreateReviewRequest(
                            userId, prodId, 3, "ok"
                    ))))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString()).get("id").asLong();

    mvc.perform(patch("/api/reviews/" + reviewId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"rating\":5,\"comment\":\"great\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.rating", is(5)))
            .andExpect(jsonPath("$.comment", is("great")));
}
}
