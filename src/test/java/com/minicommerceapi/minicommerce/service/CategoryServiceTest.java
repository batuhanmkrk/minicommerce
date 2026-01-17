package com.minicommerceapi.minicommerce.service;

import com.minicommerceapi.minicommerce.domain.Category;
import com.minicommerceapi.minicommerce.dto.CategoryDtos;
import com.minicommerceapi.minicommerce.exception.ConflictException;
import com.minicommerceapi.minicommerce.exception.NotFoundException;
import com.minicommerceapi.minicommerce.repo.CategoryRepository;
import com.minicommerceapi.minicommerce.repo.ProductRepository;
import com.minicommerceapi.minicommerce.util.SlugUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CategoryServiceTest {
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private ProductRepository productRepository;
    @InjectMocks
    private CategoryService categoryService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void create_shouldSaveAndReturnCategory() {
        CategoryDtos.CreateCategoryRequest req = new CategoryDtos.CreateCategoryRequest("Test Category");
        Category saved = new Category();
        saved.setId(1L);
        saved.setName("Test Category");
        saved.setSlug(SlugUtil.slugify("Test Category"));
        when(categoryRepository.save(any(Category.class))).thenReturn(saved);

        CategoryDtos.CategoryResponse resp = categoryService.create(req);
        assertEquals(saved.getId(), resp.id());
        assertEquals(saved.getName(), resp.name());
        assertEquals(saved.getSlug(), resp.slug());
    }

    @Test
    void list_shouldReturnAllCategories() {
        Category c = new Category();
        c.setId(1L);
        c.setName("Cat");
        c.setSlug("cat");
        when(categoryRepository.findAll()).thenReturn(List.of(c));
        List<CategoryDtos.CategoryResponse> result = categoryService.list();
        assertEquals(1, result.size());
        assertEquals("Cat", result.get(0).name());
    }

    @Test
    void get_shouldReturnCategory_whenExists() {
        Category c = new Category();
        c.setId(2L);
        c.setName("Cat2");
        c.setSlug("cat2");
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(c));
        CategoryDtos.CategoryResponse resp = categoryService.get(2L);
        assertEquals(2L, resp.id());
    }

    @Test
    void get_shouldThrowNotFound_whenNotExists() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> categoryService.get(99L));
    }

    @Test
    void update_shouldUpdateAndReturnCategory() {
        Category c = new Category();
        c.setId(3L);
        c.setName("Old");
        c.setSlug("old");
        when(categoryRepository.findById(3L)).thenReturn(Optional.of(c));
        CategoryDtos.UpdateCategoryRequest req = new CategoryDtos.UpdateCategoryRequest("New");
        CategoryDtos.CategoryResponse resp = categoryService.update(3L, req);
        assertEquals("New", resp.name());
        assertEquals(SlugUtil.slugify("New"), resp.slug());
    }

    @Test
    void update_shouldThrowNotFound_whenNotExists() {
        when(categoryRepository.findById(100L)).thenReturn(Optional.empty());
        CategoryDtos.UpdateCategoryRequest req = new CategoryDtos.UpdateCategoryRequest("X");
        assertThrows(NotFoundException.class, () -> categoryService.update(100L, req));
    }

    @Test
    void delete_shouldDelete_whenNoProducts() {
        when(categoryRepository.existsById(5L)).thenReturn(true);
        when(productRepository.existsByCategoryId(5L)).thenReturn(false);
        doNothing().when(categoryRepository).deleteById(5L);
        assertDoesNotThrow(() -> categoryService.delete(5L));
        verify(categoryRepository).deleteById(5L);
    }

    @Test
    void delete_shouldThrowNotFound_whenCategoryNotExists() {
        when(categoryRepository.existsById(6L)).thenReturn(false);
        assertThrows(NotFoundException.class, () -> categoryService.delete(6L));
    }

    @Test
    void delete_shouldThrowConflict_whenCategoryHasProducts() {
        when(categoryRepository.existsById(7L)).thenReturn(true);
        when(productRepository.existsByCategoryId(7L)).thenReturn(true);
        assertThrows(ConflictException.class, () -> categoryService.delete(7L));
    }
}
