package com.minicommerceapi.minicommerce.service;

import com.minicommerceapi.minicommerce.domain.Category;
import com.minicommerceapi.minicommerce.domain.Product;
import com.minicommerceapi.minicommerce.dto.ProductDtos;
import com.minicommerceapi.minicommerce.exception.ConflictException;
import com.minicommerceapi.minicommerce.exception.NotFoundException;
import com.minicommerceapi.minicommerce.repo.CategoryRepository;
import com.minicommerceapi.minicommerce.repo.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProductServiceTest {
    @Mock
    private ProductRepository productRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @InjectMocks
    private ProductService productService;

    @BeforeEach
    void setUp() {
        // Suppress try-with-resources warning for MockitoAnnotations in test lifecycle
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void create_shouldCreateProduct_whenValidRequest() {
        ProductDtos.CreateProductRequest req = new ProductDtos.CreateProductRequest("Test Product", "SKU123", new BigDecimal("100.0"), 10, 1L);
        Category cat = new Category();
        cat.setId(1L);
        cat.setName("TestCat");
        Product saved = new Product();
        saved.setId(1L);
        saved.setName("Test Product");
        saved.setSku("SKU123");
        saved.setPrice(new BigDecimal("100.0"));
        saved.setStock(10);
        saved.setCategory(cat);

        when(productRepository.existsBySku("SKU123")).thenReturn(false);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(cat));
        when(productRepository.save(any(Product.class))).thenReturn(saved);

        ProductDtos.ProductResponse resp = productService.create(req);
        assertEquals("Test Product", resp.name());
        assertEquals("SKU123", resp.sku());
        assertEquals(new BigDecimal("100.0"), resp.price());
        assertEquals(10, resp.stock());
        assertEquals(1L, resp.categoryId());
        assertEquals("TestCat", resp.categoryName());
    }

    @Test
    void create_shouldThrowConflictException_whenSkuExists() {
        ProductDtos.CreateProductRequest req = new ProductDtos.CreateProductRequest("Test Product", "SKU123", new BigDecimal("100.0"), 10, 1L);
        when(productRepository.existsBySku("SKU123")).thenReturn(true);
        assertThrows(ConflictException.class, () -> productService.create(req));
    }

    @Test
    void create_shouldThrowNotFoundException_whenCategoryNotFound() {
        ProductDtos.CreateProductRequest req = new ProductDtos.CreateProductRequest("Test Product", "SKU123", new BigDecimal("100.0"), 10, 1L);
        when(productRepository.existsBySku("SKU123")).thenReturn(false);
        when(categoryRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> productService.create(req));
    }

    @Test
    void list_shouldReturnAllProducts_whenCategoryIdNull() {
        Category category = new Category();
        category.setId(1L);
        category.setName("C");
        Product p = mock(Product.class);
        when(productRepository.findAll()).thenReturn(List.of(p));
        when(p.getCategory()).thenReturn(category);
        when(p.getId()).thenReturn(1L);
        when(p.getName()).thenReturn("P");
        when(p.getSku()).thenReturn("S");
        when(p.getPrice()).thenReturn(new BigDecimal("1.0"));
        when(p.getStock()).thenReturn(1);
        List<ProductDtos.ProductResponse> result = productService.list(null);
        assertEquals(1, result.size());
        ProductDtos.ProductResponse resp = result.get(0);
        assertEquals(1L, resp.categoryId());
        assertEquals("C", resp.categoryName());
    }

    @Test
    void list_shouldReturnProductsByCategory_whenCategoryIdGiven() {
        Category category = new Category();
        category.setId(1L);
        category.setName("C");
        Product p = mock(Product.class);
        when(productRepository.findByCategoryId(1L)).thenReturn(List.of(p));
        when(p.getCategory()).thenReturn(category);
        when(p.getId()).thenReturn(1L);
        when(p.getName()).thenReturn("P");
        when(p.getSku()).thenReturn("S");
        when(p.getPrice()).thenReturn(new BigDecimal("1.0"));
        when(p.getStock()).thenReturn(1);
        List<ProductDtos.ProductResponse> result = productService.list(1L);
        assertEquals(1, result.size());
        ProductDtos.ProductResponse resp = result.get(0);
        assertEquals(1L, resp.categoryId());
        assertEquals("C", resp.categoryName());
    }

    @Test
    void get_shouldReturnProduct_whenExists() {
        Category category = new Category();
        category.setId(1L);
        category.setName("C");
        Product p = mock(Product.class);
        when(productRepository.findById(1L)).thenReturn(Optional.of(p));
        when(p.getCategory()).thenReturn(category);
        when(p.getId()).thenReturn(1L);
        when(p.getName()).thenReturn("P");
        when(p.getSku()).thenReturn("S");
        when(p.getPrice()).thenReturn(new BigDecimal("1.0"));
        when(p.getStock()).thenReturn(1);
        // No need to stub getCategory().getId() or getCategory().getName() on the mock, use real Category
        ProductDtos.ProductResponse resp = productService.get(1L);
        assertEquals(1L, resp.id());
        assertEquals("P", resp.name());
        assertEquals("S", resp.sku());
        assertEquals(new BigDecimal("1.0"), resp.price());
        assertEquals(1, resp.stock());
        assertEquals(1L, resp.categoryId());
        assertEquals("C", resp.categoryName());
    }

    @Test
    void get_shouldThrowNotFoundException_whenNotExists() {
        when(productRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> productService.get(1L));
    }

    @Test
    void patch_shouldUpdateFields_whenValidRequest() {
        Product p = new Product();
        p.setId(1L);
        p.setName("Old");
        p.setSku("SKU1");
        p.setPrice(new BigDecimal("10.0"));
        p.setStock(5);
        Category c = new Category();
        c.setId(1L);
        c.setName("C");
        p.setCategory(c);
        when(productRepository.findById(1L)).thenReturn(Optional.of(p));
        when(productRepository.existsBySku("SKU2")).thenReturn(false);
        Category c2 = new Category();
        c2.setId(2L);
        c2.setName("C2");
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(c2));
        ProductDtos.PatchProductRequest req = new ProductDtos.PatchProductRequest("New", "SKU2", new BigDecimal("20.0"), 10, 2L);
        ProductDtos.ProductResponse resp = productService.patch(1L, req);
        assertEquals("New", resp.name());
        assertEquals("SKU2", resp.sku());
        assertEquals(new BigDecimal("20.0"), resp.price());
        assertEquals(10, resp.stock());
        assertEquals(2L, resp.categoryId());
        assertEquals("C2", resp.categoryName());
    }

    @Test
    void patch_shouldThrowNotFoundException_whenProductNotFound() {
        when(productRepository.findById(1L)).thenReturn(Optional.empty());
        ProductDtos.PatchProductRequest req = new ProductDtos.PatchProductRequest(null, null, null, null, null);
        assertThrows(NotFoundException.class, () -> productService.patch(1L, req));
    }

    @Test
    void patch_shouldThrowConflictException_whenSkuExists() {
        Product p = new Product();
        p.setId(1L);
        p.setSku("SKU1");
        when(productRepository.findById(1L)).thenReturn(Optional.of(p));
        when(productRepository.existsBySku("SKU2")).thenReturn(true);
        ProductDtos.PatchProductRequest req = new ProductDtos.PatchProductRequest(null, "SKU2", null, null, null);
        assertThrows(ConflictException.class, () -> productService.patch(1L, req));
    }

    @Test
    void patch_shouldThrowNotFoundException_whenCategoryNotFound() {
        Product p = new Product();
        p.setId(1L);
        p.setSku("SKU1");
        when(productRepository.findById(1L)).thenReturn(Optional.of(p));
        when(productRepository.existsBySku(anyString())).thenReturn(false);
        when(categoryRepository.findById(2L)).thenReturn(Optional.empty());
        ProductDtos.PatchProductRequest req = new ProductDtos.PatchProductRequest(null, null, null, null, 2L);
        assertThrows(NotFoundException.class, () -> productService.patch(1L, req));
    }

    @Test
    void delete_shouldDelete_whenProductExists() {
        when(productRepository.existsById(1L)).thenReturn(true);
        doNothing().when(productRepository).deleteById(1L);
        assertDoesNotThrow(() -> productService.delete(1L));
        verify(productRepository, times(1)).deleteById(1L);
    }

    @Test
    void delete_shouldThrowNotFoundException_whenProductNotExists() {
        when(productRepository.existsById(1L)).thenReturn(false);
        assertThrows(NotFoundException.class, () -> productService.delete(1L));
    }
}