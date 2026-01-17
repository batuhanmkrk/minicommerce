package com.minicommerceapi.minicommerce.e2e;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minicommerceapi.minicommerce.dto.*;
import com.minicommerceapi.minicommerce.repo.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-End Test Scenario 5: Complete Multi-User Shopping and Order Management
 *
 * This test simulates a complex real-world scenario with multiple users:
 * 1. Multiple users register
 * 2. Create product catalog
 * 3. Multiple users place different orders
 * 4. Update order statuses through lifecycle
 * 5. Users leave reviews for purchased products
 * 6. Verify order history and filtering
 *
 * This scenario represents concurrent shopping activity in an e-commerce platform.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(E2ETestConfig.class)
@Transactional
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("E2E Scenario 5: Multi-User Shopping → Orders → Reviews → Order Management")
class MultiUserShoppingE2ETest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        // Clean database for test isolation
        reviewRepository.deleteAll();
        orderRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();
    }

    /**
     * Complete multi-user shopping workflow: Multiple users register → Browse products →
     * Place orders → Update order status → Leave reviews → Verify order history
     */
    @Test
    @Order(1)
    @DisplayName("Complete multi-user shopping and order management workflow")
    void testCompleteMultiUserShoppingWorkflow() throws Exception {
        // Step 1: Register multiple users
        UserDtos.CreateUserRequest user1Request = new UserDtos.CreateUserRequest("Emma Wilson", "emma.wilson@example.com");
        MvcResult user1Result = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user1Request)))
                .andExpect(status().isCreated())
                .andReturn();
        UserDtos.UserResponse user1 = objectMapper.readValue(user1Result.getResponse().getContentAsString(), UserDtos.UserResponse.class);

        UserDtos.CreateUserRequest user2Request = new UserDtos.CreateUserRequest("Michael Brown", "michael.brown@example.com");
        MvcResult user2Result = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user2Request)))
                .andExpect(status().isCreated())
                .andReturn();
        UserDtos.UserResponse user2 = objectMapper.readValue(user2Result.getResponse().getContentAsString(), UserDtos.UserResponse.class);

        UserDtos.CreateUserRequest user3Request = new UserDtos.CreateUserRequest("Sophia Garcia", "sophia.garcia@example.com");
        MvcResult user3Result = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user3Request)))
                .andExpect(status().isCreated())
                .andReturn();
        UserDtos.UserResponse user3 = objectMapper.readValue(user3Result.getResponse().getContentAsString(), UserDtos.UserResponse.class);

        // Step 2: Create product catalog
        CategoryDtos.CreateCategoryRequest categoryRequest = new CategoryDtos.CreateCategoryRequest("Tech Gadgets");
        MvcResult categoryResult = mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(categoryRequest)))
                .andExpect(status().isCreated())
                .andReturn();
        CategoryDtos.CategoryResponse category = objectMapper.readValue(categoryResult.getResponse().getContentAsString(), CategoryDtos.CategoryResponse.class);

        // Create multiple products
        ProductDtos.CreateProductRequest product1Request = new ProductDtos.CreateProductRequest(
                "Wireless Mouse",
                "SKU-MOUSE-001",
                new BigDecimal("29.99"),
                100,
                category.id()
        );
        MvcResult product1Result = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(product1Request)))
                .andExpect(status().isCreated())
                .andReturn();
        ProductDtos.ProductResponse product1 = objectMapper.readValue(product1Result.getResponse().getContentAsString(), ProductDtos.ProductResponse.class);

        ProductDtos.CreateProductRequest product2Request = new ProductDtos.CreateProductRequest(
                "Mechanical Keyboard",
                "SKU-KEYBOARD-001",
                new BigDecimal("89.99"),
                50,
                category.id()
        );
        MvcResult product2Result = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(product2Request)))
                .andExpect(status().isCreated())
                .andReturn();
        ProductDtos.ProductResponse product2 = objectMapper.readValue(product2Result.getResponse().getContentAsString(), ProductDtos.ProductResponse.class);

        ProductDtos.CreateProductRequest product3Request = new ProductDtos.CreateProductRequest(
                "USB-C Hub",
                "SKU-HUB-001",
                new BigDecimal("39.99"),
                75,
                category.id()
        );
        MvcResult product3Result = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(product3Request)))
                .andExpect(status().isCreated())
                .andReturn();
        ProductDtos.ProductResponse product3 = objectMapper.readValue(product3Result.getResponse().getContentAsString(), ProductDtos.ProductResponse.class);

        // Step 3: All users browse products
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));

        // Step 4: User 1 places an order for mouse and keyboard
        OrderDtos.CreateOrderRequest order1Request = new OrderDtos.CreateOrderRequest(
                user1.id(),
                List.of(
                        new OrderDtos.CreateOrderItem(product1.id(), 2),
                        new OrderDtos.CreateOrderItem(product2.id(), 1)
                )
        );
        MvcResult order1Result = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(order1Request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.total").value(149.97)) // (29.99 * 2) + 89.99
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andReturn();
        OrderDtos.OrderResponse order1 = objectMapper.readValue(order1Result.getResponse().getContentAsString(), OrderDtos.OrderResponse.class);

        // Step 5: User 2 places an order for USB-C hub
        OrderDtos.CreateOrderRequest order2Request = new OrderDtos.CreateOrderRequest(
                user2.id(),
                List.of(new OrderDtos.CreateOrderItem(product3.id(), 3))
        );
        MvcResult order2Result = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(order2Request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.total").value(119.97)) // 39.99 * 3
                .andReturn();
        OrderDtos.OrderResponse order2 = objectMapper.readValue(order2Result.getResponse().getContentAsString(), OrderDtos.OrderResponse.class);

        // Step 6: User 3 places an order for all three products
        OrderDtos.CreateOrderRequest order3Request = new OrderDtos.CreateOrderRequest(
                user3.id(),
                List.of(
                        new OrderDtos.CreateOrderItem(product1.id(), 1),
                        new OrderDtos.CreateOrderItem(product2.id(), 1),
                        new OrderDtos.CreateOrderItem(product3.id(), 1)
                )
        );
        MvcResult order3Result = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(order3Request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.total").value(159.97)) // 29.99 + 89.99 + 39.99
                .andReturn();
        OrderDtos.OrderResponse order3 = objectMapper.readValue(order3Result.getResponse().getContentAsString(), OrderDtos.OrderResponse.class);

        // Step 7: Verify all orders were created
        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));

        // Step 8: Verify specific orders exist by getting them individually
        mockMvc.perform(get("/api/orders/" + order1.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(order1.id()))
                .andExpect(jsonPath("$.total").value(149.97));

        mockMvc.perform(get("/api/orders/" + order2.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(order2.id()));

        // Step 10: Update order statuses - User 1's order is paid
        OrderDtos.PatchOrderRequest paidUpdate = new OrderDtos.PatchOrderRequest("PAID");
        mockMvc.perform(patch("/api/orders/" + order1.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paidUpdate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));

        // Step 11: Update User 2's order to PAID
        OrderDtos.PatchOrderRequest paid2Update = new OrderDtos.PatchOrderRequest("PAID");
        mockMvc.perform(patch("/api/orders/" + order2.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paid2Update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));

        // Step 12: Update User 3's order to PAID
        OrderDtos.PatchOrderRequest paid3Update = new OrderDtos.PatchOrderRequest("PAID");
        mockMvc.perform(patch("/api/orders/" + order3.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paid3Update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));

        // Step 13: User 3 leaves a review for the wireless mouse (order paid)
        ReviewDtos.CreateReviewRequest review1 = new ReviewDtos.CreateReviewRequest(
                user3.id(),
                product1.id(),
                5,
                "Great mouse! Very responsive and comfortable."
        );
        MvcResult review1Result = mockMvc.perform(post("/api/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(review1)))
                .andExpect(status().isCreated())
                .andReturn();
        ReviewDtos.ReviewResponse review1Response = objectMapper.readValue(review1Result.getResponse().getContentAsString(), ReviewDtos.ReviewResponse.class);

        // Step 14: User 3 leaves a review for the keyboard
        ReviewDtos.CreateReviewRequest review2 = new ReviewDtos.CreateReviewRequest(
                user3.id(),
                product2.id(),
                4,
                "Good keyboard but a bit noisy."
        );
        mockMvc.perform(post("/api/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(review2)))
                .andExpect(status().isCreated());

        // Step 15: User 2 (with shipped order) also leaves a review
        ReviewDtos.CreateReviewRequest review3 = new ReviewDtos.CreateReviewRequest(
                user2.id(),
                product3.id(),
                5,
                "Perfect hub with all the ports I need!"
        );
        mockMvc.perform(post("/api/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(review3)))
                .andExpect(status().isCreated());

        // Step 16: View all reviews for wireless mouse
        mockMvc.perform(get("/api/reviews?productId=" + product1.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].rating").value(5));

        // Step 17: View all reviews
        mockMvc.perform(get("/api/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));

        // Step 18: Verify order status updates
        mockMvc.perform(get("/api/orders/" + order1.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));

        mockMvc.perform(get("/api/orders/" + order2.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));

        mockMvc.perform(get("/api/orders/" + order3.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));

        // Step 19: Verify product stock was reduced correctly
        mockMvc.perform(get("/api/products/" + product1.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(97)); // 100 - 2 (user1) - 1 (user3) = 97

        mockMvc.perform(get("/api/products/" + product2.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(48)); // 50 - 1 (user1) - 1 (user3) = 48

        mockMvc.perform(get("/api/products/" + product3.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(71)); // 75 - 3 (user2) - 1 (user3) = 71
    }

    /**
     * Test order cancellation and refund workflow
     */
    @Test
    @Order(2)
    @DisplayName("Order cancellation workflow")
    void testOrderCancellationWorkflow() throws Exception {
        // Create user
        UserDtos.CreateUserRequest userRequest = new UserDtos.CreateUserRequest("Oliver Smith", "oliver.smith@example.com");
        MvcResult userResult = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequest)))
                .andExpect(status().isCreated())
                .andReturn();
        UserDtos.UserResponse user = objectMapper.readValue(userResult.getResponse().getContentAsString(), UserDtos.UserResponse.class);

        // Create category and product
        CategoryDtos.CreateCategoryRequest categoryRequest = new CategoryDtos.CreateCategoryRequest("Furniture");
        MvcResult categoryResult = mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(categoryRequest)))
                .andExpect(status().isCreated())
                .andReturn();
        CategoryDtos.CategoryResponse category = objectMapper.readValue(categoryResult.getResponse().getContentAsString(), CategoryDtos.CategoryResponse.class);

        ProductDtos.CreateProductRequest productRequest = new ProductDtos.CreateProductRequest(
                "Office Chair",
                "SKU-CHAIR-001",
                new BigDecimal("299.99"),
                20,
                category.id()
        );
        MvcResult productResult = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productRequest)))
                .andExpect(status().isCreated())
                .andReturn();
        ProductDtos.ProductResponse product = objectMapper.readValue(productResult.getResponse().getContentAsString(), ProductDtos.ProductResponse.class);

        // Place order
        OrderDtos.CreateOrderRequest orderRequest = new OrderDtos.CreateOrderRequest(
                user.id(),
                List.of(new OrderDtos.CreateOrderItem(product.id(), 1))
        );
        MvcResult orderResult = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andReturn();
        OrderDtos.OrderResponse order = objectMapper.readValue(orderResult.getResponse().getContentAsString(), OrderDtos.OrderResponse.class);

        // Cancel the order
        OrderDtos.PatchOrderRequest cancelUpdate = new OrderDtos.PatchOrderRequest("CANCELLED");
        mockMvc.perform(patch("/api/orders/" + order.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cancelUpdate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        // Verify order is cancelled
        mockMvc.perform(get("/api/orders/" + order.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }
}
