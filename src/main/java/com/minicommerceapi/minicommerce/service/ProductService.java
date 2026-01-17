package com.minicommerceapi.minicommerce.service;

import com.minicommerceapi.minicommerce.domain.Category;
import com.minicommerceapi.minicommerce.domain.Product;
import com.minicommerceapi.minicommerce.dto.ProductDtos;
import com.minicommerceapi.minicommerce.exception.ConflictException;
import com.minicommerceapi.minicommerce.exception.NotFoundException;
import com.minicommerceapi.minicommerce.repo.CategoryRepository;
import com.minicommerceapi.minicommerce.repo.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductService {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional
    public ProductDtos.ProductResponse create(ProductDtos.CreateProductRequest req) {

        // TODO: Stok guncelleme senaryosunda yarista durumlar olabilir; ileride optimistic locking dusunulebilir.

        if (productRepository.existsBySku(req.sku().trim())) {
            throw new ConflictException("SKU already exists");
        }
        Category cat = categoryRepository.findById(req.categoryId())
                .orElseThrow(() -> new NotFoundException("Category not found"));

        Product p = new Product();
        p.setName(req.name().trim());
        p.setSku(req.sku().trim());
        p.setPrice(req.price());

        // TODO: Ileride buraya stok kontrolu icin concurrency (Lock veya Optimistic Lock) mekanizmasi eklememiz gerekebilir.
        // Simdilik senkronize olmayan basit kontrol yeterli.
        p.setStock(req.stock());
        p.setCategory(cat);
        p = productRepository.save(p);
        return toResponse(p);
    }

    @Transactional(readOnly = true)
    public List<ProductDtos.ProductResponse> list(Long categoryId) {
        List<Product> products = (categoryId == null)
                ? productRepository.findAll()
                : productRepository.findByCategoryId(categoryId);
        return products.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ProductDtos.ProductResponse get(Long id) {
        Product p = productRepository.findById(id).orElseThrow(() -> new NotFoundException("Product not found"));
        return toResponse(p);
    }

    @Transactional
    public ProductDtos.ProductResponse patch(Long id, ProductDtos.PatchProductRequest req) {
        Product p = productRepository.findById(id).orElseThrow(() -> new NotFoundException("Product not found"));

        if (req.name() != null) p.setName(req.name().trim());

        if (req.sku() != null) {
            String sku = req.sku().trim();
            if (!p.getSku().equals(sku) && productRepository.existsBySku(sku)) {
                throw new ConflictException("SKU already exists");
            }
            p.setSku(sku);
        }

        if (req.price() != null) p.setPrice(req.price());

        // Not: Stok guncellemeleri gercek hayatta kritik. Burada sadece ornek proje oldugu icin basit set yapiyoruz.
        if (req.stock() != null) p.setStock(req.stock());
        if (req.categoryId() != null) {
            Category cat = categoryRepository.findById(req.categoryId())
                    .orElseThrow(() -> new NotFoundException("Category not found"));
            p.setCategory(cat);
        }
        return toResponse(p);
    }

    @Transactional
    public void delete(Long id) {
        if (!productRepository.existsById(id)) {
            throw new NotFoundException("Product not found");
        }
        productRepository.deleteById(id);
    }

    public ProductDtos.ProductResponse toResponse(Product p) {
        Category c = p.getCategory();
        return new ProductDtos.ProductResponse(
                p.getId(),
                p.getName(),
                p.getSku(),
                p.getPrice(),
                p.getStock(),
                c.getId(),
                c.getName()
        );
    }
}
