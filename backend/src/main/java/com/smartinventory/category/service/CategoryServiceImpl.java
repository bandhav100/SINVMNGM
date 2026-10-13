package com.smartinventory.category.service;

import com.smartinventory.category.dto.CategoryRequestDto;
import com.smartinventory.category.dto.CategoryResponseDto;
import com.smartinventory.category.entity.Category;
import com.smartinventory.category.repository.CategoryRepository;
import com.smartinventory.common.dto.PageResponseDto;
import com.smartinventory.common.exception.DuplicateResourceException;
import com.smartinventory.common.exception.ResourceNotFoundException;
import com.smartinventory.company.service.CurrentCompanyProvider;
import com.smartinventory.company.entity.Company;
import com.smartinventory.product.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final CurrentCompanyProvider currentCompanyProvider;

    @Autowired
    public CategoryServiceImpl(
            CategoryRepository categoryRepository,
            ProductRepository productRepository,
            CurrentCompanyProvider currentCompanyProvider
    ) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.currentCompanyProvider = currentCompanyProvider;
    }

    public CategoryServiceImpl(
            CategoryRepository categoryRepository,
            ProductRepository productRepository
    ) {
        this(categoryRepository, productRepository, null);
    }

    @Override
    public CategoryResponseDto createCategory(CategoryRequestDto request) {
        Company company = currentCompanyProvider != null ? currentCompanyProvider.getCurrentCompany() : null;
        String trimmedName = request.name().trim();
        boolean exists = (currentCompanyProvider != null && currentCompanyProvider.getCurrentCompanyId() != null)
                ? categoryRepository.existsByCompanyIdAndNameIgnoreCase(currentCompanyProvider.getCurrentCompanyId(), trimmedName)
                : categoryRepository.existsByNameIgnoreCase(trimmedName);
        if (exists) {
            throw new DuplicateResourceException("Category with name '" + trimmedName + "' already exists");
        }

        Category category = new Category();
        if (company != null) {
            category.setCompany(company);
        }
        category.setName(trimmedName);
        category.setDescription(request.description() != null ? request.description().trim() : null);

        Category saved = categoryRepository.save(category);
        return CategoryResponseDto.fromEntity(saved, 0L);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponseDto getCategoryById(Long id) {
        Category category = (currentCompanyProvider != null && currentCompanyProvider.getCurrentCompanyId() != null)
                ? categoryRepository.findByIdAndCompanyId(id, currentCompanyProvider.getCurrentCompanyId())
                        .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id))
                : categoryRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));

        long productCount = (currentCompanyProvider != null && currentCompanyProvider.getCurrentCompanyId() != null)
                ? productRepository.countByCompanyIdAndCategoryId(currentCompanyProvider.getCurrentCompanyId(), id)
                : productRepository.countByCategoryId(id);

        return CategoryResponseDto.fromEntity(category, productCount);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDto<CategoryResponseDto> getCategories(String search, Pageable pageable) {
        if (currentCompanyProvider != null && currentCompanyProvider.getCurrentCompanyId() != null) {
            Long companyId = currentCompanyProvider.getCurrentCompanyId();
            Page<Category> page;
            if (search != null && !search.trim().isEmpty()) {
                page = categoryRepository.searchCategories(companyId, search.trim(), pageable);
            } else {
                page = categoryRepository.findAllByCompanyId(companyId, pageable);
            }
            return PageResponseDto.fromPage(page.map(cat -> {
                long count = productRepository.countByCompanyIdAndCategoryId(companyId, cat.getId());
                return CategoryResponseDto.fromEntity(cat, count);
            }));
        } else {
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
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponseDto> getAllCategories() {
        if (currentCompanyProvider != null && currentCompanyProvider.getCurrentCompanyId() != null) {
            Long companyId = currentCompanyProvider.getCurrentCompanyId();
            return categoryRepository.findAllByCompanyId(companyId, Pageable.unpaged()).getContent().stream()
                    .map(cat -> {
                        long count = productRepository.countByCompanyIdAndCategoryId(companyId, cat.getId());
                        return CategoryResponseDto.fromEntity(cat, count);
                    })
                    .toList();
        } else {
            return categoryRepository.findAll().stream()
                    .map(cat -> {
                        long count = productRepository.countByCategoryId(cat.getId());
                        return CategoryResponseDto.fromEntity(cat, count);
                    })
                    .toList();
        }
    }

    @Override
    public CategoryResponseDto updateCategory(Long id, CategoryRequestDto request) {
        Category category = (currentCompanyProvider != null && currentCompanyProvider.getCurrentCompanyId() != null)
                ? categoryRepository.findByIdAndCompanyId(id, currentCompanyProvider.getCurrentCompanyId())
                        .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id))
                : categoryRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));

        String trimmedName = request.name().trim();
        boolean exists = (currentCompanyProvider != null && currentCompanyProvider.getCurrentCompanyId() != null)
                ? categoryRepository.existsByCompanyIdAndNameIgnoreCaseAndIdNot(currentCompanyProvider.getCurrentCompanyId(), trimmedName, id)
                : categoryRepository.existsByNameIgnoreCaseAndIdNot(trimmedName, id);
        if (exists) {
            throw new DuplicateResourceException("Category with name '" + trimmedName + "' already exists on another category");
        }

        category.setName(trimmedName);
        category.setDescription(request.description() != null ? request.description().trim() : null);

        Category updated = categoryRepository.save(category);
        long productCount = (currentCompanyProvider != null && currentCompanyProvider.getCurrentCompanyId() != null)
                ? productRepository.countByCompanyIdAndCategoryId(currentCompanyProvider.getCurrentCompanyId(), id)
                : productRepository.countByCategoryId(id);
        return CategoryResponseDto.fromEntity(updated, productCount);
    }

    @Override
    public void deleteCategory(Long id) {
        Category category = (currentCompanyProvider != null && currentCompanyProvider.getCurrentCompanyId() != null)
                ? categoryRepository.findByIdAndCompanyId(id, currentCompanyProvider.getCurrentCompanyId())
                        .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id))
                : categoryRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));

        long productCount = (currentCompanyProvider != null && currentCompanyProvider.getCurrentCompanyId() != null)
                ? productRepository.countByCompanyIdAndCategoryId(currentCompanyProvider.getCurrentCompanyId(), id)
                : productRepository.countByCategoryId(id);
        if (productCount > 0) {
            throw new IllegalStateException("Cannot delete category with associated products. Reassign products first.");
        }

        categoryRepository.delete(category);
    }

    @Override
    @Transactional(readOnly = true)
    public long countProductsByCategoryId(Long id) {
        if (currentCompanyProvider != null && currentCompanyProvider.getCurrentCompanyId() != null) {
            return productRepository.countByCompanyIdAndCategoryId(currentCompanyProvider.getCurrentCompanyId(), id);
        }
        return productRepository.countByCategoryId(id);
    }
}
