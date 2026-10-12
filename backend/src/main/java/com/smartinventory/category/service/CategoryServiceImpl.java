package com.smartinventory.category.service;

import com.smartinventory.category.dto.CategoryRequestDto;
import com.smartinventory.category.dto.CategoryResponseDto;
import com.smartinventory.category.entity.Category;
import com.smartinventory.category.repository.CategoryRepository;
import com.smartinventory.common.dto.PageResponseDto;
import com.smartinventory.common.exception.DuplicateResourceException;
import com.smartinventory.common.exception.ResourceNotFoundException;
import com.smartinventory.product.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public CategoryServiceImpl(CategoryRepository categoryRepository, ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    @Override
    public CategoryResponseDto createCategory(CategoryRequestDto request) {
        String trimmedName = request.name().trim();
        if (categoryRepository.existsByNameIgnoreCase(trimmedName)) {
            throw new DuplicateResourceException("Category with name '" + trimmedName + "' already exists");
        }

        Category category = new Category();
        category.setName(trimmedName);
        category.setDescription(request.description() != null ? request.description().trim() : null);

        Category saved = categoryRepository.save(category);
        return CategoryResponseDto.fromEntity(saved, 0L);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponseDto getCategoryById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
        long productCount = productRepository.countByCategoryId(id);
        return CategoryResponseDto.fromEntity(category, productCount);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDto<CategoryResponseDto> getCategories(String search, Pageable pageable) {
        Page<Category> page;
        if (search != null && !search.trim().isEmpty()) {
            page = categoryRepository.searchCategories(search.trim(), pageable);
        } else {
            page = categoryRepository.findAll(pageable);
        }

        return PageResponseDto.fromPage(page.map(cat -> {
            long count = productRepository.countByCategoryId(cat.getId());
            return CategoryResponseDto.fromEntity(cat, count);
        }));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponseDto> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(cat -> {
                    long count = productRepository.countByCategoryId(cat.getId());
                    return CategoryResponseDto.fromEntity(cat, count);
                })
                .toList();
    }

    @Override
    public CategoryResponseDto updateCategory(Long id, CategoryRequestDto request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));

        String trimmedName = request.name().trim();
        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(trimmedName, id)) {
            throw new DuplicateResourceException("Category with name '" + trimmedName + "' already exists on another category");
        }

        category.setName(trimmedName);
        category.setDescription(request.description() != null ? request.description().trim() : null);

        Category updated = categoryRepository.save(category);
        long productCount = productRepository.countByCategoryId(id);
        return CategoryResponseDto.fromEntity(updated, productCount);
    }

    @Override
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));

        // When deleted, database foreign key ON DELETE SET NULL disassociates products safely
        categoryRepository.delete(category);
    }

    @Override
    @Transactional(readOnly = true)
    public long countProductsByCategoryId(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new ResourceNotFoundException("Category not found with id: " + id);
        }
        return productRepository.countByCategoryId(id);
    }
}
