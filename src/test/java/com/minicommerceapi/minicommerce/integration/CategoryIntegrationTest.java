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
class CategoryIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
    }

    @Test
    void testCreateCategory_Success_PostMethod() throws Exception {
        CategoryDtos.CreateCategoryRequest request = new CategoryDtos.CreateCategoryRequest("Electronics");

        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Electronics"))
                .andExpect(jsonPath("$.slug").value("electronics"));
    }

    @Test
    void testGetCategory_Success() throws Exception {
        // Create category first
        CategoryDtos.CreateCategoryRequest createReq = new CategoryDtos.CreateCategoryRequest("Books");

        MvcResult createResult = mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        CategoryDtos.CategoryResponse createdCategory = objectMapper.readValue(
                createResult.getResponse().getContentAsString(),
                CategoryDtos.CategoryResponse.class
        );

        // GET the category
        mockMvc.perform(get("/api/categories/" + createdCategory.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(createdCategory.id()))
                .andExpect(jsonPath("$.name").value("Books"))
                .andExpect(jsonPath("$.slug").value("books"));
    }

    @Test
    void testGetCategory_NotFound_ErrorScenario() throws Exception {
        mockMvc.perform(get("/api/categories/99999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testListCategories_Success() throws Exception {
        // Create multiple categories
        CategoryDtos.CreateCategoryRequest cat1 = new CategoryDtos.CreateCategoryRequest("Category1");
        CategoryDtos.CreateCategoryRequest cat2 = new CategoryDtos.CreateCategoryRequest("Category2");

        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cat1)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cat2)))
                .andExpect(status().isCreated());

        // List all categories
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].name", containsInAnyOrder("Category1", "Category2")));
    }

    @Test
    void testUpdateCategory_Success_PutMethod() throws Exception {
        // Create category (using PUT for update as per CategoryController)
        CategoryDtos.CreateCategoryRequest createReq = new CategoryDtos.CreateCategoryRequest("Original Name");

        MvcResult createResult = mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        CategoryDtos.CategoryResponse createdCategory = objectMapper.readValue(
                createResult.getResponse().getContentAsString(),
                CategoryDtos.CategoryResponse.class
        );

        // Update category (PUT)
        CategoryDtos.UpdateCategoryRequest updateReq = new CategoryDtos.UpdateCategoryRequest("Updated Name");

        mockMvc.perform(put("/api/categories/" + createdCategory.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(createdCategory.id()))
                .andExpect(jsonPath("$.name").value("Updated Name"))
                .andExpect(jsonPath("$.slug").value("updated-name"));
    }

    @Test
    void testDeleteCategory_Success_DeleteMethod() throws Exception {
        // Create category
        CategoryDtos.CreateCategoryRequest createReq = new CategoryDtos.CreateCategoryRequest("To Delete");

        MvcResult createResult = mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        CategoryDtos.CategoryResponse createdCategory = objectMapper.readValue(
                createResult.getResponse().getContentAsString(),
                CategoryDtos.CategoryResponse.class
        );

        // DELETE the category
        mockMvc.perform(delete("/api/categories/" + createdCategory.id()))
                .andExpect(status().isNoContent());

        // Verify it's deleted
        mockMvc.perform(get("/api/categories/" + createdCategory.id()))
                .andExpect(status().isNotFound());
    }

    @Test
    void testCreateCategory_BlankName_BadRequest() throws Exception {
        CategoryDtos.CreateCategoryRequest invalidReq = new CategoryDtos.CreateCategoryRequest("");

        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidReq)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testDeleteCategory_WithProducts_Conflict_ErrorScenario() throws Exception {
        // Create category
        CategoryDtos.CreateCategoryRequest categoryReq = new CategoryDtos.CreateCategoryRequest("With Products");
        MvcResult categoryResult = mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(categoryReq)))
                .andExpect(status().isCreated())
                .andReturn();
        CategoryDtos.CategoryResponse category = objectMapper.readValue(
                categoryResult.getResponse().getContentAsString(),
                CategoryDtos.CategoryResponse.class
        );

        // Create product in this category
        ProductDtos.CreateProductRequest productReq = new ProductDtos.CreateProductRequest(
                "Product",
                "SKU-PROD",
                new BigDecimal("100.00"),
                10,
                category.id()
        );
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productReq)))
                .andExpect(status().isCreated());

        // Try to delete category (should fail due to products)
        mockMvc.perform(delete("/api/categories/" + category.id()))
                .andExpect(status().isConflict());
    }
}
