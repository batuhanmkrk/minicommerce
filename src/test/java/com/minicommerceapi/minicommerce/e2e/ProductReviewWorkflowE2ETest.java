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
 * End-to-End Test Scenario 3: Complete Review and Rating Workflow
 *
 * This test simulates a complete product review and rating flow:
 * 1. User registers
 * 2. Create category and products
 * 3. User places an order for a product
 * 4. User submits a review for the product
 * 5. User updates their review
 * 6. View all reviews for the product
 * 7. User deletes their review
 *
 * This scenario represents real-world e-commerce behavior where customers
 * purchase products and leave feedback.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(E2ETestConfig.class)
@Transactional
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("E2E Scenario 3: Purchase → Review → Update Review → Delete Review")
class ProductReviewWorkflowE2ETest {

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
     * Complete review workflow: User registration → Product purchase → Review submission →
     * Review update → Review deletion
     */
    @Test
    @Order(1)
    @DisplayName("Complete product review workflow from purchase to review management")
    void testCompleteProductReviewWorkflow() throws Exception {
        // Step 1: Create a user
        UserDtos.CreateUserRequest userRequest = new UserDtos.CreateUserRequest(
                "Sarah Johnson",
                "sarah.johnson@example.com"
        );

        MvcResult userResult = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        UserDtos.UserResponse user = objectMapper.readValue(
                userResult.getResponse().getContentAsString(),
                UserDtos.UserResponse.class
        );

        // Step 2: Create a category
        CategoryDtos.CreateCategoryRequest categoryRequest = new CategoryDtos.CreateCategoryRequest("Electronics");
        MvcResult categoryResult = mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(categoryRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        CategoryDtos.CategoryResponse category = objectMapper.readValue(
                categoryResult.getResponse().getContentAsString(),
                CategoryDtos.CategoryResponse.class
        );

        // Step 3: Create a product
        ProductDtos.CreateProductRequest productRequest = new ProductDtos.CreateProductRequest(
                "Wireless Headphones",
                "SKU-HEADPHONES-001",
                new BigDecimal("149.99"),
                30,
                category.id()
        );

        MvcResult productResult = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        ProductDtos.ProductResponse product = objectMapper.readValue(
                productResult.getResponse().getContentAsString(),
                ProductDtos.ProductResponse.class
        );

        // Step 4: User places an order for the product
        OrderDtos.CreateOrderRequest orderRequest = new OrderDtos.CreateOrderRequest(
                user.id(),
                List.of(new OrderDtos.CreateOrderItem(product.id(), 1))
        );

        MvcResult orderResult = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.total").value(149.99))
                .andReturn();

        OrderDtos.OrderResponse order = objectMapper.readValue(
                orderResult.getResponse().getContentAsString(),
                OrderDtos.OrderResponse.class
        );

        // Step 5: Check reviews before submission - should be empty
        mockMvc.perform(get("/api/reviews?productId=" + product.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        // Step 6: User submits a review
        ReviewDtos.CreateReviewRequest reviewRequest = new ReviewDtos.CreateReviewRequest(
                user.id(),
                product.id(),
                5,
                "Excellent sound quality! Highly recommend these headphones."
        );

        MvcResult reviewResult = mockMvc.perform(post("/api/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reviewRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.userId").value(user.id()))
                .andExpect(jsonPath("$.productId").value(product.id()))
                .andExpect(jsonPath("$.rating").value(5))
                .andExpect(jsonPath("$.comment").value("Excellent sound quality! Highly recommend these headphones."))
                .andReturn();

        ReviewDtos.ReviewResponse review = objectMapper.readValue(
                reviewResult.getResponse().getContentAsString(),
                ReviewDtos.ReviewResponse.class
        );

        // Step 7: View product reviews
        mockMvc.perform(get("/api/reviews?productId=" + product.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(review.id()))
                .andExpect(jsonPath("$[0].rating").value(5));

        // Step 8: User updates their review (changed opinion after more use)
        ReviewDtos.PatchReviewRequest updateRequest = new ReviewDtos.PatchReviewRequest(
                4,
                "Very good headphones, but battery life could be better. Still recommend!"
        );

        mockMvc.perform(patch("/api/reviews/" + review.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(review.id()))
                .andExpect(jsonPath("$.rating").value(4))
                .andExpect(jsonPath("$.comment").value("Very good headphones, but battery life could be better. Still recommend!"));

        // Step 9: Verify updated review
        mockMvc.perform(get("/api/reviews/" + review.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(4))
                .andExpect(jsonPath("$.comment").value("Very good headphones, but battery life could be better. Still recommend!"));

        // Step 10: Get reviews by user
        mockMvc.perform(get("/api/reviews?userId=" + user.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(review.id()));

        // Step 11: User deletes their review
        mockMvc.perform(delete("/api/reviews/" + review.id()))
                .andExpect(status().isNoContent());

        // Step 12: Verify review is deleted
        mockMvc.perform(get("/api/reviews/" + review.id()))
                .andExpect(status().isNotFound());

        // Step 13: Product reviews should be empty again
        mockMvc.perform(get("/api/reviews?productId=" + product.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    /**
     * Test multiple users reviewing the same product
     */
    @Test
    @Order(2)
    @DisplayName("Multiple users review the same product")
    void testMultipleUsersReviewingProduct() throws Exception {
        // Create category and product
        CategoryDtos.CreateCategoryRequest categoryRequest = new CategoryDtos.CreateCategoryRequest("Books");
        MvcResult categoryResult = mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(categoryRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        CategoryDtos.CategoryResponse category = objectMapper.readValue(
                categoryResult.getResponse().getContentAsString(),
                CategoryDtos.CategoryResponse.class
        );

        ProductDtos.CreateProductRequest productRequest = new ProductDtos.CreateProductRequest(
                "Java Programming Guide",
                "SKU-BOOK-JAVA-001",
                new BigDecimal("59.99"),
                100,
                category.id()
        );

        MvcResult productResult = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        ProductDtos.ProductResponse product = objectMapper.readValue(
                productResult.getResponse().getContentAsString(),
                ProductDtos.ProductResponse.class
        );

        // Create three users
        UserDtos.CreateUserRequest user1Request = new UserDtos.CreateUserRequest("Alice Brown", "alice.brown@example.com");
        MvcResult user1Result = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user1Request)))
                .andExpect(status().isCreated())
                .andReturn();
        UserDtos.UserResponse user1 = objectMapper.readValue(user1Result.getResponse().getContentAsString(), UserDtos.UserResponse.class);

        UserDtos.CreateUserRequest user2Request = new UserDtos.CreateUserRequest("Bob Wilson", "bob.wilson@example.com");
        MvcResult user2Result = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user2Request)))
                .andExpect(status().isCreated())
                .andReturn();
        UserDtos.UserResponse user2 = objectMapper.readValue(user2Result.getResponse().getContentAsString(), UserDtos.UserResponse.class);

        UserDtos.CreateUserRequest user3Request = new UserDtos.CreateUserRequest("Carol Davis", "carol.davis@example.com");
        MvcResult user3Result = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user3Request)))
                .andExpect(status().isCreated())
                .andReturn();
        UserDtos.UserResponse user3 = objectMapper.readValue(user3Result.getResponse().getContentAsString(), UserDtos.UserResponse.class);

        // Each user submits a review
        ReviewDtos.CreateReviewRequest review1 = new ReviewDtos.CreateReviewRequest(user1.id(), product.id(), 5, "Perfect for beginners!");
        mockMvc.perform(post("/api/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(review1)))
                .andExpect(status().isCreated());

        ReviewDtos.CreateReviewRequest review2 = new ReviewDtos.CreateReviewRequest(user2.id(), product.id(), 4, "Good content, but a bit basic.");
        mockMvc.perform(post("/api/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(review2)))
                .andExpect(status().isCreated());

        ReviewDtos.CreateReviewRequest review3 = new ReviewDtos.CreateReviewRequest(user3.id(), product.id(), 5, "Comprehensive and well-written!");
        mockMvc.perform(post("/api/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(review3)))
                .andExpect(status().isCreated());

        // Verify all reviews for the product
        mockMvc.perform(get("/api/reviews?productId=" + product.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[*].rating", containsInAnyOrder(5, 4, 5)));
    }
}
