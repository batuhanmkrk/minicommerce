package edu.akademik.minicommerce.service;

import edu.akademik.minicommerce.domain.Category;
import edu.akademik.minicommerce.dto.CategoryDtos;
import edu.akademik.minicommerce.exception.ConflictException;
import edu.akademik.minicommerce.exception.NotFoundException;
import edu.akademik.minicommerce.repo.CategoryRepository;
import edu.akademik.minicommerce.repo.ProductRepository;
import edu.akademik.minicommerce.util.SlugUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public CategoryService(CategoryRepository categoryRepository, ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public CategoryDtos.CategoryResponse create(CategoryDtos.CreateCategoryRequest req) {
        Category c = new Category();
        c.setName(req.name().trim());
        c.setSlug(SlugUtil.slugify(req.name()));
        c = categoryRepository.save(c);
        return toResponse(c);
    }

    @Transactional(readOnly = true)
    public List<CategoryDtos.CategoryResponse> list() {
        return categoryRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public CategoryDtos.CategoryResponse get(Long id) {
        Category c = categoryRepository.findById(id).orElseThrow(() -> new NotFoundException("Category not found"));
        return toResponse(c);
    }

    @Transactional
    public CategoryDtos.CategoryResponse update(Long id, CategoryDtos.UpdateCategoryRequest req) {
        Category c = categoryRepository.findById(id).orElseThrow(() -> new NotFoundException("Category not found"));
        c.setName(req.name().trim());
        c.setSlug(SlugUtil.slugify(req.name()));
        return toResponse(c);
    }

    /**
     * Deletion rule (RESTRICT):
     * A category cannot be deleted if it still contains products.
     * This avoids accidental data loss and is easy to demonstrate with a 409 Conflict test.
     */
    @Transactional
    public void delete(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new NotFoundException("Category not found");
        }
        if (productRepository.existsByCategoryId(id)) {
            throw new ConflictException("Category has products; delete or move products first");
        }
        categoryRepository.deleteById(id);
    }

    private CategoryDtos.CategoryResponse toResponse(Category c) {
        return new CategoryDtos.CategoryResponse(c.getId(), c.getName(), c.getSlug());
    }
}
