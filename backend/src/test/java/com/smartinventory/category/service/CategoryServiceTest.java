package com.smartinventory.category.service;

import com.smartinventory.category.dto.CategoryRequestDto;
import com.smartinventory.category.dto.CategoryResponseDto;
import com.smartinventory.category.entity.Category;
import com.smartinventory.category.repository.CategoryRepository;
import com.smartinventory.common.dto.PageResponseDto;
import com.smartinventory.common.exception.DuplicateResourceException;
import com.smartinventory.common.exception.ResourceNotFoundException;
import com.smartinventory.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    private Category sampleCategory;
    private CategoryRequestDto sampleRequest;

    @BeforeEach
    void setUp() {
        sampleCategory = new Category(1L, "Electronics", "Electronic components and devices");
        sampleCategory.setCreatedAt(Instant.now());
        sampleCategory.setUpdatedAt(Instant.now());

        sampleRequest = new CategoryRequestDto("Electronics", "Electronic components and devices");
    }

    @Test
    @DisplayName("createCategory should persist and return DTO when name is unique")
    void createCategory_Success() {
        when(categoryRepository.existsByNameIgnoreCase("Electronics")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(sampleCategory);

        CategoryResponseDto response = categoryService.createCategory(sampleRequest);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Electronics");
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    @DisplayName("createCategory should throw DuplicateResourceException on case-insensitive duplicate")
    void createCategory_DuplicateName_ThrowsException() {
        when(categoryRepository.existsByNameIgnoreCase("Electronics")).thenReturn(true);

        assertThatThrownBy(() -> categoryService.createCategory(sampleRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Electronics");

        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    @DisplayName("getCategoryById should return category with product count")
    void getCategoryById_Success() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(sampleCategory));
        when(productRepository.countByCategoryId(1L)).thenReturn(5L);

        CategoryResponseDto response = categoryService.getCategoryById(1L);

        assertThat(response).isNotNull();
        assertThat(response.productCount()).isEqualTo(5L);
    }

    @Test
    @DisplayName("getCategoryById should throw ResourceNotFoundException when ID does not exist")
    void getCategoryById_NotFound_ThrowsException() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.getCategoryById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("getCategories should return paginated list")
    void getCategories_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        when(categoryRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(sampleCategory)));
        when(productRepository.countByCategoryId(1L)).thenReturn(3L);

        PageResponseDto<CategoryResponseDto> response = categoryService.getCategories(null, pageable);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).productCount()).isEqualTo(3L);
    }

    @Test
    @DisplayName("deleteCategory should delete entity when found")
    void deleteCategory_Success() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(sampleCategory));

        categoryService.deleteCategory(1L);

        verify(categoryRepository).delete(sampleCategory);
    }
}
