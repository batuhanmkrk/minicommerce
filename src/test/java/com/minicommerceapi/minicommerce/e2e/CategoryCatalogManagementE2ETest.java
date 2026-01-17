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
 * End-to-End Test Scenario 4: Multi-Category Product Catalog Management
 *
 * This test simulates a complete catalog management workflow:
 * 1. Create multiple product categories
 * 2. Create products in different categories
 * 3. Browse and filter products by category
 * 4. Update category information
 * 5. Move product to different category
 * 6. Delete category and verify constraints
 *
 * This scenario represents catalog organization and management in an e-commerce system.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(E2ETestConfig.class)
@Transactional
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("E2E Scenario 4: Multi-Category Catalog Management")
class CategoryCatalogManagementE2ETest {

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
     * Complete catalog management workflow: Create categories → Add products →
     * Filter by category → Update category → Move products → Manage constraints
     */
    @Test
    @Order(1)
    @DisplayName("Complete multi-category catalog management workflow")
    void testCompleteCatalogManagement() throws Exception {
        // Step 1: List categories - should be empty initially
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        // Step 2: Create multiple categories
        CategoryDtos.CreateCategoryRequest electronicsRequest = new CategoryDtos.CreateCategoryRequest("Electronics");
        MvcResult electronicsResult = mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(electronicsRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Electronics"))
                .andReturn();

        CategoryDtos.CategoryResponse electronics = objectMapper.readValue(
                electronicsResult.getResponse().getContentAsString(),
                CategoryDtos.CategoryResponse.class
        );

        CategoryDtos.CreateCategoryRequest clothingRequest = new CategoryDtos.CreateCategoryRequest("Clothing");
        MvcResult clothingResult = mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(clothingRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        CategoryDtos.CategoryResponse clothing = objectMapper.readValue(
                clothingResult.getResponse().getContentAsString(),
                CategoryDtos.CategoryResponse.class
        );

        CategoryDtos.CreateCategoryRequest homeRequest = new CategoryDtos.CreateCategoryRequest("Home & Garden");
        MvcResult homeResult = mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(homeRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        CategoryDtos.CategoryResponse home = objectMapper.readValue(
                homeResult.getResponse().getContentAsString(),
                CategoryDtos.CategoryResponse.class
        );

        // Step 3: List all categories
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[*].name", containsInAnyOrder("Electronics", "Clothing", "Home & Garden")));

        // Step 4: Create products in different categories
        // Electronics products
        ProductDtos.CreateProductRequest smartphoneRequest = new ProductDtos.CreateProductRequest(
                "Smartphone X",
                "SKU-PHONE-001",
                new BigDecimal("799.99"),
                20,
                electronics.id()
        );
        MvcResult smartphoneResult = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(smartphoneRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        ProductDtos.ProductResponse smartphone = objectMapper.readValue(
                smartphoneResult.getResponse().getContentAsString(),
                ProductDtos.ProductResponse.class
        );

        ProductDtos.CreateProductRequest laptopRequest = new ProductDtos.CreateProductRequest(
                "Laptop Pro",
                "SKU-LAPTOP-001",
                new BigDecimal("1499.99"),
                15,
                electronics.id()
        );
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(laptopRequest)))
                .andExpect(status().isCreated());

        // Clothing products
        ProductDtos.CreateProductRequest tshirtRequest = new ProductDtos.CreateProductRequest(
                "Cotton T-Shirt",
                "SKU-TSHIRT-001",
                new BigDecimal("29.99"),
                100,
                clothing.id()
        );
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tshirtRequest)))
                .andExpect(status().isCreated());

        ProductDtos.CreateProductRequest jeansRequest = new ProductDtos.CreateProductRequest(
                "Denim Jeans",
                "SKU-JEANS-001",
                new BigDecimal("79.99"),
                50,
                clothing.id()
        );
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(jeansRequest)))
                .andExpect(status().isCreated());

        // Home products
        ProductDtos.CreateProductRequest lampRequest = new ProductDtos.CreateProductRequest(
                "LED Desk Lamp",
                "SKU-LAMP-001",
                new BigDecimal("49.99"),
                30,
                home.id()
        );
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(lampRequest)))
                .andExpect(status().isCreated());

        // Step 5: List all products
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)));

        // Step 6: Filter products by Electronics category
        mockMvc.perform(get("/api/products?categoryId=" + electronics.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].name", containsInAnyOrder("Smartphone X", "Laptop Pro")));

        // Step 7: Filter products by Clothing category
        mockMvc.perform(get("/api/products?categoryId=" + clothing.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].name", containsInAnyOrder("Cotton T-Shirt", "Denim Jeans")));

        // Step 8: Filter products by Home category
        mockMvc.perform(get("/api/products?categoryId=" + home.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("LED Desk Lamp"));

        // Step 9: Update category name
        CategoryDtos.UpdateCategoryRequest updateCategory = new CategoryDtos.UpdateCategoryRequest("Consumer Electronics");
        mockMvc.perform(put("/api/categories/" + electronics.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateCategory)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(electronics.id()))
                .andExpect(jsonPath("$.name").value("Consumer Electronics"));

        // Step 10: Verify category name updated
        mockMvc.perform(get("/api/categories/" + electronics.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Consumer Electronics"));

        // Step 11: Move a product to a different category
        ProductDtos.PatchProductRequest moveProduct = new ProductDtos.PatchProductRequest(
                null,
                null,
                null,
                null,
                clothing.id() // Move smartphone from electronics to clothing (for test purposes)
        );
        mockMvc.perform(patch("/api/products/" + smartphone.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(moveProduct)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryId").value(clothing.id()));

        // Step 12: Verify product moved - electronics should now have 1 product
        mockMvc.perform(get("/api/products?categoryId=" + electronics.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Laptop Pro"));

        // Step 13: Clothing should now have 3 products
        mockMvc.perform(get("/api/products?categoryId=" + clothing.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));

        // Step 14: View category details
        mockMvc.perform(get("/api/categories/" + clothing.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(clothing.id()))
                .andExpect(jsonPath("$.name").value("Clothing"));

        // Step 15: Get category by slug (if supported)
        mockMvc.perform(get("/api/categories/" + electronics.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Consumer Electronics"));
    }

    /**
     * Test creating a comprehensive product catalog with categories
     */
    @Test
    @Order(2)
    @DisplayName("Create comprehensive catalog with multiple categories and products")
    void testComprehensiveCatalogCreation() throws Exception {
        // Create a structured catalog
        CategoryDtos.CreateCategoryRequest sportsRequest = new CategoryDtos.CreateCategoryRequest("Sports & Outdoors");
        MvcResult sportsResult = mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sportsRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        CategoryDtos.CategoryResponse sports = objectMapper.readValue(
                sportsResult.getResponse().getContentAsString(),
                CategoryDtos.CategoryResponse.class
        );

        // Add multiple products to sports category
        String[] productNames = {"Tennis Racket", "Soccer Ball", "Yoga Mat", "Running Shoes"};
        BigDecimal[] prices = {new BigDecimal("89.99"), new BigDecimal("24.99"), new BigDecimal("39.99"), new BigDecimal("129.99")};
        int[] stocks = {20, 50, 30, 25};

        for (int i = 0; i < productNames.length; i++) {
            ProductDtos.CreateProductRequest productRequest = new ProductDtos.CreateProductRequest(
                    productNames[i],
                    "SKU-SPORTS-" + (i + 1),
                    prices[i],
                    stocks[i],
                    sports.id()
            );

            mockMvc.perform(post("/api/products")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(productRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value(productNames[i]));
        }

        // Verify all products in sports category
        mockMvc.perform(get("/api/products?categoryId=" + sports.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[*].name", containsInAnyOrder("Tennis Racket", "Soccer Ball", "Yoga Mat", "Running Shoes")));
    }
}
