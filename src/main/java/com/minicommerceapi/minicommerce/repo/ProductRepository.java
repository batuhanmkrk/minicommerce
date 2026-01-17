package com.minicommerceapi.minicommerce.repo;

import com.minicommerceapi.minicommerce.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
    boolean existsByCategoryId(Long categoryId);
    List<Product> findByCategoryId(Long categoryId);
    boolean existsBySku(String sku);
}
