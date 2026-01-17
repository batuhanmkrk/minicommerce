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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-End Test Scenario 2: Complete Product Lifecycle Management
 *
 * This test simulates a complete product management flow:
 * 1. List all products (empty initially)
 * 2. Create a category for the product
 * 3. Create a new product
 * 4. View product details
 * 5. Update product information (price and stock)
 * 6. List all products (verify product appears)
 * 7. Delete the product
 * 8. Verify product is deleted
 *
 * Each test creates its own independent data set to ensure test isolation.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(E2ETestConfig.class)
@Transactional
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("E2E Scenario 2: Product Listing → Create → View → Update → Delete")
class ProductLifecycleE2ETest {

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
     * Complete product lifecycle: List → Create → View → Update → Delete → Verify
     */
    @Test
    @Order(1)
    @DisplayName("Complete product lifecycle from creation to deletion")
    void testCompleteProductLifecycle() throws Exception {
        // Step 1: List all products - should be empty initially
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        // Step 2: Create a category for the product
        CategoryDtos.CreateCategoryRequest categoryRequest = new CategoryDtos.CreateCategoryRequest("Clothing");
        MvcResult categoryResult = mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(categoryRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Clothing"))
                .andReturn();

        CategoryDtos.CategoryResponse category = objectMapper.readValue(
                categoryResult.getResponse().getContentAsString(),
                CategoryDtos.CategoryResponse.class
        );

        // Step 3: Create a new product
        ProductDtos.CreateProductRequest productRequest = new ProductDtos.CreateProductRequest(
                "Designer Jacket",
                "SKU-JACKET-001",
                new BigDecimal("249.99"),
                25,
                category.id()
        );

        MvcResult createResult = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Designer Jacket"))
                .andExpect(jsonPath("$.sku").value("SKU-JACKET-001"))
                .andExpect(jsonPath("$.price").value(249.99))
                .andExpect(jsonPath("$.stock").value(25))
                .andExpect(jsonPath("$.categoryId").value(category.id()))
                .andReturn();

        ProductDtos.ProductResponse product = objectMapper.readValue(
                createResult.getResponse().getContentAsString(),
                ProductDtos.ProductResponse.class
        );

        // Step 4: View product details
        mockMvc.perform(get("/api/products/" + product.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(product.id()))
                .andExpect(jsonPath("$.name").value("Designer Jacket"))
                .andExpect(jsonPath("$.sku").value("SKU-JACKET-001"))
                .andExpect(jsonPath("$.price").value(249.99))
                .andExpect(jsonPath("$.stock").value(25));

        // Step 5: Update product - change price and reduce stock
        ProductDtos.PatchProductRequest updateRequest = new ProductDtos.PatchProductRequest(
                null, // name unchanged
                null, // sku unchanged
                new BigDecimal("199.99"), // discounted price
                20, // reduced stock
                null // category unchanged
        );

        mockMvc.perform(patch("/api/products/" + product.id())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(product.id()))
                .andExpect(jsonPath("$.name").value("Designer Jacket")) // unchanged
                .andExpect(jsonPath("$.price").value(199.99)) // updated
                .andExpect(jsonPath("$.stock").value(20)); // updated

        // Step 6: List all products - verify our product appears
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(product.id()))
                .andExpect(jsonPath("$[0].name").value("Designer Jacket"))
                .andExpect(jsonPath("$[0].price").value(199.99));

        // Step 7: Filter products by category
        mockMvc.perform(get("/api/products?categoryId=" + category.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(product.id()));

        // Step 8: Delete the product
        mockMvc.perform(delete("/api/products/" + product.id()))
                .andExpect(status().isNoContent());

        // Step 9: Verify product is deleted - should return 404
        mockMvc.perform(get("/api/products/" + product.id()))
                .andExpect(status().isNotFound());

        // Step 10: List all products - should be empty again
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    /**
     * Test updating multiple products and managing inventory
     */
    @Test
    @Order(2)
    @DisplayName("Manage multiple products and inventory")
    void testMultipleProductsInventoryManagement() throws Exception {
        // Create category
        CategoryDtos.CreateCategoryRequest categoryRequest = new CategoryDtos.CreateCategoryRequest("Accessories");
        MvcResult categoryResult = mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(categoryRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        CategoryDtos.CategoryResponse category = objectMapper.readValue(
                categoryResult.getResponse().getContentAsString(),
                CategoryDtos.CategoryResponse.class
        );

        // Create multiple products
        ProductDtos.CreateProductRequest product1Request = new ProductDtos.CreateProductRequest(
                "Watch",
                "SKU-WATCH-001",
                new BigDecimal("199.99"),
                10,
                category.id()
        );

        MvcResult product1Result = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(product1Request)))
                .andExpect(status().isCreated())
                .andReturn();

        ProductDtos.ProductResponse product1 = objectMapper.readValue(
                product1Result.getResponse().getContentAsString(),
                ProductDtos.ProductResponse.class
        );

        ProductDtos.CreateProductRequest product2Request = new ProductDtos.CreateProductRequest(
                "Sunglasses",
                "SKU-GLASSES-001",
                new BigDecimal("89.99"),
                15,
                category.id()
        );

        MvcResult product2Result = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(product2Request)))
                .andExpect(status().isCreated())
                .andReturn();

        ProductDtos.ProductResponse product2 = objectMapper.readValue(
                product2Result.getResponse().getContentAsString(),
                ProductDtos.ProductResponse.class
        );

        // List all products
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        // Update product1 stock
        ProductDtos.PatchProductRequest updateStock = new ProductDtos.PatchProductRequest(
                null,
                null,
                null,
                5, // reduce stock to 5
                null
        );

        mockMvc.perform(patch("/api/products/" + product1.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateStock)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(5));

        // Verify stock updated
        mockMvc.perform(get("/api/products/" + product1.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(5));
    }
}
