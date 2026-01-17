package com.minicommerceapi.minicommerce.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minicommerceapi.minicommerce.dto.UserDtos;
import com.minicommerceapi.minicommerce.dto.OrderDtos;
import com.minicommerceapi.minicommerce.dto.ProductDtos;
import com.minicommerceapi.minicommerce.dto.CategoryDtos;
import com.minicommerceapi.minicommerce.repo.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

@SpringBootTest
@ActiveProfiles("test")
@Import(IntegrationTestConfig.class)
@Transactional
class OrderIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        orderRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();
        categoryRepository.deleteAll();
    }

    @Test
    void testGetOrder_Success() throws Exception {
        // Setup: Create user, category, product, and order
        UserDtos.CreateUserRequest userReq = new UserDtos.CreateUserRequest("Jane Smith", "jane@example.com");
        MvcResult userResult = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userReq)))
                .andExpect(status().isCreated())
                .andReturn();
        UserDtos.UserResponse user = objectMapper.readValue(userResult.getResponse().getContentAsString(), UserDtos.UserResponse.class);

        CategoryDtos.CreateCategoryRequest categoryReq = new CategoryDtos.CreateCategoryRequest("Books");
        MvcResult categoryResult = mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(categoryReq)))
                .andExpect(status().isCreated())
                .andReturn();
        CategoryDtos.CategoryResponse category = objectMapper.readValue(categoryResult.getResponse().getContentAsString(), CategoryDtos.CategoryResponse.class);

        ProductDtos.CreateProductRequest productReq = new ProductDtos.CreateProductRequest("Book", "SKU-BOOK-001", new BigDecimal("25.00"), 100, category.id());
        MvcResult productResult = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productReq)))
                .andExpect(status().isCreated())
                .andReturn();
        ProductDtos.ProductResponse product = objectMapper.readValue(productResult.getResponse().getContentAsString(), ProductDtos.ProductResponse.class);

        OrderDtos.CreateOrderRequest orderReq = new OrderDtos.CreateOrderRequest(
                user.id(),
                List.of(new OrderDtos.CreateOrderItem(product.id(), 3))
        );
        MvcResult orderResult = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderReq)))
                .andExpect(status().isCreated())
                .andReturn();
        OrderDtos.OrderResponse order = objectMapper.readValue(orderResult.getResponse().getContentAsString(), OrderDtos.OrderResponse.class);

        // Test GET order
        mockMvc.perform(get("/api/orders/" + order.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(order.id()))
                .andExpect(jsonPath("$.userId").value(user.id()))
                .andExpect(jsonPath("$.total").value(75.00));
    }

    @Test
    void testGetOrder_NotFound_ErrorScenario() throws Exception {
        mockMvc.perform(get("/api/orders/99999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testListOrders_Success() throws Exception {
        // Create user, category, product
        UserDtos.CreateUserRequest userReq = new UserDtos.CreateUserRequest("Test User", "test@example.com");
        MvcResult userResult = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userReq)))
                .andExpect(status().isCreated())
                .andReturn();
        UserDtos.UserResponse user = objectMapper.readValue(userResult.getResponse().getContentAsString(), UserDtos.UserResponse.class);

        CategoryDtos.CreateCategoryRequest categoryReq = new CategoryDtos.CreateCategoryRequest("Test Cat");
        MvcResult categoryResult = mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(categoryReq)))
                .andExpect(status().isCreated())
                .andReturn();
        CategoryDtos.CategoryResponse category = objectMapper.readValue(categoryResult.getResponse().getContentAsString(), CategoryDtos.CategoryResponse.class);

        ProductDtos.CreateProductRequest productReq = new ProductDtos.CreateProductRequest("Test Product", "SKU-TEST", new BigDecimal("10.00"), 100, category.id());
        MvcResult productResult = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productReq)))
                .andExpect(status().isCreated())
                .andReturn();
        ProductDtos.ProductResponse product = objectMapper.readValue(productResult.getResponse().getContentAsString(), ProductDtos.ProductResponse.class);

        // Create two orders
        OrderDtos.CreateOrderRequest order1Req = new OrderDtos.CreateOrderRequest(user.id(), List.of(new OrderDtos.CreateOrderItem(product.id(), 1)));
        OrderDtos.CreateOrderRequest order2Req = new OrderDtos.CreateOrderRequest(user.id(), List.of(new OrderDtos.CreateOrderItem(product.id(), 2)));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(order1Req)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(order2Req)))
                .andExpect(status().isCreated());

        // List all orders
        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }


    @Test
    void testDeleteOrder_Success_DeleteMethod() throws Exception {
        // Create order
        UserDtos.CreateUserRequest userReq = new UserDtos.CreateUserRequest("Delete User", "delete@example.com");
        MvcResult userResult = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userReq)))
                .andExpect(status().isCreated())
                .andReturn();
        UserDtos.UserResponse user = objectMapper.readValue(userResult.getResponse().getContentAsString(), UserDtos.UserResponse.class);

        CategoryDtos.CreateCategoryRequest categoryReq = new CategoryDtos.CreateCategoryRequest("Del Category");
        MvcResult categoryResult = mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(categoryReq)))
                .andExpect(status().isCreated())
                .andReturn();
        CategoryDtos.CategoryResponse category = objectMapper.readValue(categoryResult.getResponse().getContentAsString(), CategoryDtos.CategoryResponse.class);

        ProductDtos.CreateProductRequest productReq = new ProductDtos.CreateProductRequest("Del Product", "SKU-DEL", new BigDecimal("50.00"), 10, category.id());
        MvcResult productResult = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productReq)))
                .andExpect(status().isCreated())
                .andReturn();
        ProductDtos.ProductResponse product = objectMapper.readValue(productResult.getResponse().getContentAsString(), ProductDtos.ProductResponse.class);

        OrderDtos.CreateOrderRequest orderReq = new OrderDtos.CreateOrderRequest(user.id(), List.of(new OrderDtos.CreateOrderItem(product.id(), 1)));
        MvcResult orderResult = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderReq)))
                .andExpect(status().isCreated())
                .andReturn();
        OrderDtos.OrderResponse order = objectMapper.readValue(orderResult.getResponse().getContentAsString(), OrderDtos.OrderResponse.class);

        // DELETE order
        mockMvc.perform(delete("/api/orders/" + order.id()))
                .andExpect(status().isNoContent());

        // Verify deleted
        mockMvc.perform(get("/api/orders/" + order.id()))
                .andExpect(status().isNotFound());
    }

    @Test
    void testCreateOrder_UserNotFound_ErrorScenario() throws Exception {
        // Try to create order with non-existent user
        OrderDtos.CreateOrderRequest orderReq = new OrderDtos.CreateOrderRequest(
                99999L,
                List.of(new OrderDtos.CreateOrderItem(1L, 1))
        );

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderReq)))
                .andExpect(status().isNotFound());
    }

    @Test
    void testCreateOrder_InvalidQuantity_BadRequest() throws Exception {
        // Create valid user first
        UserDtos.CreateUserRequest userReq = new UserDtos.CreateUserRequest("Valid User", "valid@example.com");
        MvcResult userResult = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userReq)))
                .andExpect(status().isCreated())
                .andReturn();
        UserDtos.UserResponse user = objectMapper.readValue(userResult.getResponse().getContentAsString(), UserDtos.UserResponse.class);

        // Try to create order with invalid quantity (0)
        OrderDtos.CreateOrderRequest invalidOrderReq = new OrderDtos.CreateOrderRequest(
                user.id(),
                List.of(new OrderDtos.CreateOrderItem(1L, 0))
        );

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidOrderReq)))
                .andExpect(status().isBadRequest());
    }
}
