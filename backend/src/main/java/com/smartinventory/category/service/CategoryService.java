package com.smartinventory.category.service;

import com.smartinventory.category.dto.CategoryRequestDto;
import com.smartinventory.category.dto.CategoryResponseDto;
import com.smartinventory.common.dto.PageResponseDto;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CategoryService {
    CategoryResponseDto createCategory(CategoryRequestDto request);
    CategoryResponseDto getCategoryById(Long id);
    PageResponseDto<CategoryResponseDto> getCategories(String search, Pageable pageable);
    List<CategoryResponseDto> getAllCategories();
    CategoryResponseDto updateCategory(Long id, CategoryRequestDto request);
    void deleteCategory(Long id);
    long countProductsByCategoryId(Long id);
}
