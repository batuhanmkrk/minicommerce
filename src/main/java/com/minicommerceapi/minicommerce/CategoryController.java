package com.minicommerceapi.minicommerce;

import com.minicommerceapi.minicommerce.dto.CategoryDtos;
import com.minicommerceapi.minicommerce.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {
    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @Operation(summary = "Create a category")
    @PostMapping
    public ResponseEntity<CategoryDtos.CategoryResponse> create(@Valid @RequestBody CategoryDtos.CreateCategoryRequest req) {
        CategoryDtos.CategoryResponse created = categoryService.create(req);
        return ResponseEntity.created(URI.create("/api/categories/" + created.id())).body(created);
    }

    @Operation(summary = "List categories")
    @GetMapping
    public List<CategoryDtos.CategoryResponse> list() {
        return categoryService.list();
    }

    @Operation(summary = "Get category by id")
    @GetMapping("/{id}")
    public CategoryDtos.CategoryResponse get(@PathVariable Long id) {
        return categoryService.get(id);
    }

    @Operation(summary = "Update category (PUT)")
    @PutMapping("/{id}")
    public CategoryDtos.CategoryResponse update(@PathVariable Long id, @Valid @RequestBody CategoryDtos.UpdateCategoryRequest req) {
        return categoryService.update(id, req);
    }

    @Operation(summary = "Delete category (restricted if it has products)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
