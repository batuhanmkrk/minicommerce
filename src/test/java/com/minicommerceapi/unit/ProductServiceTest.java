package com.minicommerceapi.unit;

import com.minicommerceapi.domain.Category;
import com.minicommerceapi.domain.Product;
import com.minicommerceapi.dto.ProductDtos;
import com.minicommerceapi.exception.ConflictException;
import com.minicommerceapi.exception.NotFoundException;
import com.minicommerceapi.repo.CategoryRepository;
import com.minicommerceapi.repo.ProductRepository;
import com.minicommerceapi.service.ProductService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProductServiceTest {

    @Test
    void create_missingCategory_throwsNotFound() {
        ProductRepository pr = mock(ProductRepository.class);
        CategoryRepository cr = mock(CategoryRepository.class);

        when(pr.existsBySku("SKU1")).thenReturn(false);
        when(cr.findById(1L)).thenReturn(Optional.empty());

        ProductService svc = new ProductService(pr, cr);
        assertThrows(NotFoundException.class, () -> svc.create(new ProductDtos.CreateProductRequest(
                "P", "SKU1", new BigDecimal("10.00"), 5, 1L
        )));
    }

    @Test
    void create_duplicateSku_throwsConflict() {
        ProductRepository pr = mock(ProductRepository.class);
        CategoryRepository cr = mock(CategoryRepository.class);
        when(pr.existsBySku("DUP")).thenReturn(true);

        ProductService svc = new ProductService(pr, cr);
        assertThrows(ConflictException.class, () -> svc.create(new ProductDtos.CreateProductRequest(
                "P", "DUP", new BigDecimal("10.00"), 5, 1L
        )));
    }

    @Test
    void patch_updatesOnlyProvidedFields() {
        ProductRepository pr = mock(ProductRepository.class);
        CategoryRepository cr = mock(CategoryRepository.class);

        Category cat = new Category(); cat.setName("C"); cat.setSlug("c");
        Product p = new Product();
        p.setName("Old"); p.setSku("SKU"); p.setPrice(new BigDecimal("5.00")); p.setStock(3); p.setCategory(cat);

        when(pr.findById(1L)).thenReturn(Optional.of(p));
        when(pr.existsBySku("NEW")).thenReturn(false);

        ProductService svc = new ProductService(pr, cr);
        var res = svc.patch(1L, new ProductDtos.PatchProductRequest(
                " New ", "NEW", new BigDecimal("7.50"), null, null
        ));

        assertEquals("New", res.name());
        assertEquals("NEW", res.sku());
        assertEquals(new BigDecimal("7.50"), res.price());
        assertEquals(3, res.stock()); // unchanged
    }

    @Test
    void patch_duplicateSku_throwsConflict() {
        ProductRepository pr = mock(ProductRepository.class);
        CategoryRepository cr = mock(CategoryRepository.class);

        Category cat = new Category(); cat.setName("C"); cat.setSlug("c");
        Product p = new Product();
        p.setName("Old"); p.setSku("SKU"); p.setPrice(new BigDecimal("5.00")); p.setStock(3); p.setCategory(cat);

        when(pr.findById(1L)).thenReturn(Optional.of(p));
        when(pr.existsBySku("DUP")).thenReturn(true);

        ProductService svc = new ProductService(pr, cr);
        assertThrows(ConflictException.class, () -> svc.patch(1L, new ProductDtos.PatchProductRequest(
                null, "DUP", null, null, null
        )));
    }
}
