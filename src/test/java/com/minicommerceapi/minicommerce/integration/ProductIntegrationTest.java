package com.minicommerceapi.minicommerce.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minicommerceapi.minicommerce.dto.CategoryDtos;
import com.minicommerceapi.minicommerce.dto.ProductDtos;
import com.minicommerceapi.minicommerce.repo.CategoryRepository;
import com.minicommerceapi.minicommerce.repo.ProductRepository;
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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(IntegrationTestConfig.class)
@Transactional
class ProductIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
    }

    @Test
    void testCreateProduct_Success_PostMethod() throws Exception {
        // First create a category
        CategoryDtos.CreateCategoryRequest categoryReq = new CategoryDtos.CreateCategoryRequest("Electronics");
        MvcResult categoryResult = mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(categoryReq)))
                .andExpect(status().isCreated())
                .andReturn();

        CategoryDtos.CategoryResponse category = objectMapper.readValue(
                categoryResult.getResponse().getContentAsString(),
                CategoryDtos.CategoryResponse.class
        );

        // Create a product
        ProductDtos.CreateProductRequest productReq = new ProductDtos.CreateProductRequest(
                "Laptop",
                "SKU-LAPTOP-001",
                new BigDecimal("999.99"),
                50,
                category.id()
        );

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Laptop"))
                .andExpect(jsonPath("$.sku").value("SKU-LAPTOP-001"))
                .andExpect(jsonPath("$.price").value(999.99))
                .andExpect(jsonPath("$.stock").value(50))
                .andExpect(jsonPath("$.categoryId").value(category.id()))
                .andExpect(jsonPath("$.categoryName").value("Electronics"));
    }

    @Test
    void testGetProduct_Success() throws Exception {
        // Create category and product
        CategoryDtos.CreateCategoryRequest categoryReq = new CategoryDtos.CreateCategoryRequest("Books");
        MvcResult categoryResult = mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(categoryReq)))
                .andExpect(status().isCreated())
                .andReturn();

        CategoryDtos.CategoryResponse category = objectMapper.readValue(
                categoryResult.getResponse().getContentAsString(),
                CategoryDtos.CategoryResponse.class
        );

        ProductDtos.CreateProductRequest productReq = new ProductDtos.CreateProductRequest(
                "Java Programming",
                "SKU-BOOK-001",
                new BigDecimal("45.00"),
                100,
                category.id()
        );

        MvcResult productResult = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productReq)))
                .andExpect(status().isCreated())
                .andReturn();

        ProductDtos.ProductResponse product = objectMapper.readValue(
                productResult.getResponse().getContentAsString(),
                ProductDtos.ProductResponse.class
        );

        // GET the product
        mockMvc.perform(get("/api/products/" + product.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(product.id()))
                .andExpect(jsonPath("$.name").value("Java Programming"))
                .andExpect(jsonPath("$.sku").value("SKU-BOOK-001"))
                .andExpect(jsonPath("$.price").value(45.00));
    }

    @Test
    void testGetProduct_NotFound_ErrorScenario() throws Exception {
        // GET non-existent product
        mockMvc.perform(get("/api/products/99999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testListProducts_Success() throws Exception {
        // Create category and multiple products
        CategoryDtos.CreateCategoryRequest categoryReq = new CategoryDtos.CreateCategoryRequest("Phones");
        MvcResult categoryResult = mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(categoryReq)))
                .andExpect(status().isCreated())
                .andReturn();

        CategoryDtos.CategoryResponse category = objectMapper.readValue(
                categoryResult.getResponse().getContentAsString(),
                CategoryDtos.CategoryResponse.class
        );

        // Create two products
        ProductDtos.CreateProductRequest product1 = new ProductDtos.CreateProductRequest(
                "iPhone 13",
                "SKU-PHONE-001",
                new BigDecimal("799.99"),
                30,
                category.id()
        );

        ProductDtos.CreateProductRequest product2 = new ProductDtos.CreateProductRequest(
                "Samsung Galaxy",
                "SKU-PHONE-002",
                new BigDecimal("699.99"),
                25,
                category.id()
        );

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(product1)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(product2)))
                .andExpect(status().isCreated());

        // List all products
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].name", containsInAnyOrder("iPhone 13", "Samsung Galaxy")));
    }

    @Test
    void testPatchProduct_Success_PatchMethod() throws Exception {
        // Create category and product
        CategoryDtos.CreateCategoryRequest categoryReq = new CategoryDtos.CreateCategoryRequest("Accessories");
        MvcResult categoryResult = mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(categoryReq)))
                .andExpect(status().isCreated())
                .andReturn();

        CategoryDtos.CategoryResponse category = objectMapper.readValue(
                categoryResult.getResponse().getContentAsString(),
                CategoryDtos.CategoryResponse.class
        );

        ProductDtos.CreateProductRequest productReq = new ProductDtos.CreateProductRequest(
                "Mouse",
                "SKU-MOUSE-001",
                new BigDecimal("25.00"),
                100,
                category.id()
        );

        MvcResult productResult = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productReq)))
                .andExpect(status().isCreated())
                .andReturn();

        ProductDtos.ProductResponse product = objectMapper.readValue(
                productResult.getResponse().getContentAsString(),
                ProductDtos.ProductResponse.class
        );

        // PATCH the product
        ProductDtos.PatchProductRequest patchReq = new ProductDtos.PatchProductRequest(
                "Wireless Mouse",
                null,
                new BigDecimal("35.00"),
                null,
                null
        );

        mockMvc.perform(patch("/api/products/" + product.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(patchReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Wireless Mouse"))
                .andExpect(jsonPath("$.price").value(35.00))
                .andExpect(jsonPath("$.sku").value("SKU-MOUSE-001")); // Unchanged
    }

    @Test
    void testDeleteProduct_Success_DeleteMethod() throws Exception {
        // Create category and product
        CategoryDtos.CreateCategoryRequest categoryReq = new CategoryDtos.CreateCategoryRequest("Software");
        MvcResult categoryResult = mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(categoryReq)))
                .andExpect(status().isCreated())
                .andReturn();

        CategoryDtos.CategoryResponse category = objectMapper.readValue(
                categoryResult.getResponse().getContentAsString(),
                CategoryDtos.CategoryResponse.class
        );

        ProductDtos.CreateProductRequest productReq = new ProductDtos.CreateProductRequest(
                "Antivirus",
                "SKU-AV-001",
                new BigDecimal("49.99"),
                200,
                category.id()
        );

        MvcResult productResult = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productReq)))
                .andExpect(status().isCreated())
                .andReturn();

        ProductDtos.ProductResponse product = objectMapper.readValue(
                productResult.getResponse().getContentAsString(),
                ProductDtos.ProductResponse.class
        );

        // DELETE the product
        mockMvc.perform(delete("/api/products/" + product.id()))
                .andExpect(status().isNoContent());

        // Verify it's deleted
        mockMvc.perform(get("/api/products/" + product.id()))
                .andExpect(status().isNotFound());
    }

    @Test
    void testCreateProduct_InvalidData_BadRequest() throws Exception {
        // Create product with invalid price (negative)
        ProductDtos.CreateProductRequest invalidReq = new ProductDtos.CreateProductRequest(
                "Invalid Product",
                "SKU-INVALID",
                new BigDecimal("-10.00"),
                10,
                1L
        );

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidReq)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testFilterProductsByCategory_DatabaseOperations() throws Exception {
        // Create two categories
        CategoryDtos.CreateCategoryRequest category1Req = new CategoryDtos.CreateCategoryRequest("Category1");
        MvcResult category1Result = mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(category1Req)))
                .andExpect(status().isCreated())
                .andReturn();

        CategoryDtos.CategoryResponse category1 = objectMapper.readValue(
                category1Result.getResponse().getContentAsString(),
                CategoryDtos.CategoryResponse.class
        );

        CategoryDtos.CreateCategoryRequest category2Req = new CategoryDtos.CreateCategoryRequest("Category2");
        MvcResult category2Result = mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(category2Req)))
                .andExpect(status().isCreated())
                .andReturn();

        CategoryDtos.CategoryResponse category2 = objectMapper.readValue(
                category2Result.getResponse().getContentAsString(),
                CategoryDtos.CategoryResponse.class
        );

        // Create products in different categories
        ProductDtos.CreateProductRequest product1 = new ProductDtos.CreateProductRequest(
                "Product1", "SKU1", new BigDecimal("10.00"), 10, category1.id()
        );
        ProductDtos.CreateProductRequest product2 = new ProductDtos.CreateProductRequest(
                "Product2", "SKU2", new BigDecimal("20.00"), 20, category2.id()
        );

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(product1)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(product2)))
                .andExpect(status().isCreated());

        // Filter by category1
        mockMvc.perform(get("/api/products")
                        .param("categoryId", category1.id().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Product1"));
    }
}
