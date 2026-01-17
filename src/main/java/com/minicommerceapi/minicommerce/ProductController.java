package com.minicommerceapi.minicommerce;

import com.minicommerceapi.minicommerce.dto.ProductDtos;
import com.minicommerceapi.minicommerce.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @Operation(summary = "Create a product")
    @PostMapping
    public ResponseEntity<ProductDtos.ProductResponse> create(@Valid @RequestBody ProductDtos.CreateProductRequest req) {
        ProductDtos.ProductResponse created = productService.create(req);
        return ResponseEntity.created(URI.create("/api/products/" + created.id())).body(created);
    }

    @Operation(summary = "List products (optional filter by categoryId)")
    @GetMapping
    public List<ProductDtos.ProductResponse> list(@RequestParam(required = false) Long categoryId) {
        return productService.list(categoryId);
    }

    @Operation(summary = "Get product by id")
    @GetMapping("/{id}")
    public ProductDtos.ProductResponse get(@PathVariable Long id) {
        return productService.get(id);
    }

    @Operation(summary = "Patch product")
    @PatchMapping("/{id}")
    public ProductDtos.ProductResponse patch(@PathVariable Long id, @Valid @RequestBody ProductDtos.PatchProductRequest req) {
        return productService.patch(id, req);
    }

    @Operation(summary = "Delete product")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
