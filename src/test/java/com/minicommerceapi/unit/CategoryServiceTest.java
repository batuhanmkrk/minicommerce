package com.minicommerceapi.unit;

import com.minicommerceapi.exception.ConflictException;
import com.minicommerceapi.repo.CategoryRepository;
import com.minicommerceapi.repo.ProductRepository;
import com.minicommerceapi.service.CategoryService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class CategoryServiceTest {

    @Test
    void delete_categoryWithProducts_throwsConflict() {
        CategoryRepository catRepo = mock(CategoryRepository.class);
        ProductRepository prodRepo = mock(ProductRepository.class);

        when(catRepo.existsById(10L)).thenReturn(true);
        when(prodRepo.existsByCategoryId(10L)).thenReturn(true);

        CategoryService svc = new CategoryService(catRepo, prodRepo);
        assertThrows(ConflictException.class, () -> svc.delete(10L));
    }
}
