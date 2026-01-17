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
 * End-to-End Test Scenario 1: Complete User Registration and Order Workflow
 *
 * This test simulates a complete e-commerce flow from user registration to order completion:
 * 1. Create a new user (registration)
 * 2. Create product categories
 * 3. Create products in those categories
 * 4. User places an order with multiple products
 * 5. Verify order details and status
 * 6. Update order status (payment processing)
 *
 * Each test creates its own independent data set to ensure test isolation.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(E2ETestConfig.class)
@Transactional
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("E2E Scenario 1: User Registration → Product Creation → Order Placement")
class UserRegistrationToOrderE2ETest {

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
     * Complete workflow: User registration → Category creation → Product creation →
     * Order placement → Payment processing
     */
    @Test
    @Order(1)
    @DisplayName("Complete order workflow from user registration to payment")
    void testCompleteUserRegistrationToOrderWorkflow() throws Exception {
        // Step 1: Register a new user
        UserDtos.CreateUserRequest userRequest = new UserDtos.CreateUserRequest(
                "John Doe",
                "john.doe@example.com"
        );

        MvcResult userResult = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("John Doe"))
                .andExpect(jsonPath("$.email").value("john.doe@example.com"))
                .andReturn();

        UserDtos.UserResponse user = objectMapper.readValue(
                userResult.getResponse().getContentAsString(),
                UserDtos.UserResponse.class
        );

        // Step 2: Create product categories
        CategoryDtos.CreateCategoryRequest electronicsCategory = new CategoryDtos.CreateCategoryRequest("Electronics");
        MvcResult categoryResult1 = mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(electronicsCategory)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Electronics"))
                .andReturn();

        CategoryDtos.CategoryResponse electronics = objectMapper.readValue(
                categoryResult1.getResponse().getContentAsString(),
                CategoryDtos.CategoryResponse.class
        );

        CategoryDtos.CreateCategoryRequest booksCategory = new CategoryDtos.CreateCategoryRequest("Books");
        MvcResult categoryResult2 = mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(booksCategory)))
                .andExpect(status().isCreated())
                .andReturn();

        CategoryDtos.CategoryResponse books = objectMapper.readValue(
                categoryResult2.getResponse().getContentAsString(),
                CategoryDtos.CategoryResponse.class
        );

        // Step 3: Create products in different categories
        ProductDtos.CreateProductRequest laptopRequest = new ProductDtos.CreateProductRequest(
                "Gaming Laptop",
                "SKU-LAPTOP-001",
                new BigDecimal("1299.99"),
                15,
                electronics.id()
        );

        MvcResult laptopResult = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(laptopRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Gaming Laptop"))
                .andExpect(jsonPath("$.price").value(1299.99))
                .andExpect(jsonPath("$.stock").value(15))
                .andReturn();

        ProductDtos.ProductResponse laptop = objectMapper.readValue(
                laptopResult.getResponse().getContentAsString(),
                ProductDtos.ProductResponse.class
        );

        ProductDtos.CreateProductRequest bookRequest = new ProductDtos.CreateProductRequest(
                "Spring Boot in Action",
                "SKU-BOOK-001",
                new BigDecimal("49.99"),
                50,
                books.id()
        );

        MvcResult bookResult = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Spring Boot in Action"))
                .andReturn();

        ProductDtos.ProductResponse book = objectMapper.readValue(
                bookResult.getResponse().getContentAsString(),
                ProductDtos.ProductResponse.class
        );

        // Step 4: User places an order with multiple products
        OrderDtos.CreateOrderRequest orderRequest = new OrderDtos.CreateOrderRequest(
                user.id(),
                List.of(
                        new OrderDtos.CreateOrderItem(laptop.id(), 1),
                        new OrderDtos.CreateOrderItem(book.id(), 2)
                )
        );

        MvcResult orderResult = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.userId").value(user.id()))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.total").value(1399.97)) // 1299.99 + (49.99 * 2)
                .andReturn();

        OrderDtos.OrderResponse order = objectMapper.readValue(
                orderResult.getResponse().getContentAsString(),
                OrderDtos.OrderResponse.class
        );

        // Step 5: Verify order details
        mockMvc.perform(get("/api/orders/" + order.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(order.id()))
                .andExpect(jsonPath("$.userId").value(user.id()))
                .andExpect(jsonPath("$.total").value(1399.97))
                .andExpect(jsonPath("$.items", hasSize(2)));

        // Step 6: Update order status (simulate payment processing)
        OrderDtos.PatchOrderRequest paymentUpdate = new OrderDtos.PatchOrderRequest("PAID");

        mockMvc.perform(patch("/api/orders/" + order.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentUpdate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(order.id()))
                .andExpect(jsonPath("$.status").value("PAID"));

        // Step 7: Verify final order status
        mockMvc.perform(get("/api/orders/" + order.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));

        // Verify user can see their orders (all orders in this test belong to this user)
        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(order.id()));
    }
}
