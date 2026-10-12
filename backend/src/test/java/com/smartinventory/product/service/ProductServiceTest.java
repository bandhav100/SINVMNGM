package com.smartinventory.product.service;

import com.smartinventory.category.repository.CategoryRepository;
import com.smartinventory.common.dto.PageResponseDto;
import com.smartinventory.common.exception.DuplicateResourceException;
import com.smartinventory.common.exception.ResourceNotFoundException;
import com.smartinventory.product.dto.ProductRequestDto;
import com.smartinventory.product.dto.ProductResponseDto;
import com.smartinventory.product.entity.Product;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    private Product sampleProduct;
    private ProductRequestDto sampleRequest;

    @BeforeEach
    void setUp() {
        sampleProduct = new Product(1L, "Wireless Mouse", "WM-001", "Ergonomic wireless mouse",
                new BigDecimal("29.99"), 150, 20);
        sampleProduct.setCreatedAt(Instant.now());
        sampleProduct.setUpdatedAt(Instant.now());

        sampleRequest = new ProductRequestDto("Wireless Mouse", "WM-001", "Ergonomic wireless mouse",
                new BigDecimal("29.99"), 150, 20, null);
    }

    @Test
    @DisplayName("createProduct should persist and return created product DTO")
    void createProduct_Success() {
        when(productRepository.existsBySkuIgnoreCase("WM-001")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenReturn(sampleProduct);

        ProductResponseDto result = productService.createProduct(sampleRequest);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.sku()).isEqualTo("WM-001");
        assertThat(result.name()).isEqualTo("Wireless Mouse");
        assertThat(result.price()).isEqualByComparingTo("29.99");
        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("createProduct should throw DuplicateResourceException if SKU already exists")
    void createProduct_DuplicateSku_ThrowsException() {
        when(productRepository.existsBySkuIgnoreCase("WM-001")).thenReturn(true);

        assertThatThrownBy(() -> productService.createProduct(sampleRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("WM-001");

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("getProductById should return product when found")
    void getProductById_Success() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(sampleProduct));

        ProductResponseDto result = productService.getProductById(1L);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.sku()).isEqualTo("WM-001");
    }

    @Test
    @DisplayName("getProductById should throw ResourceNotFoundException when product does not exist")
    void getProductById_NotFound_ThrowsException() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    @DisplayName("getProducts without search query should call findAll")
    void getProducts_NoSearch_ReturnsPagedProducts() {
        Pageable pageable = PageRequest.of(0, 10);
        when(productRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(sampleProduct)));

        PageResponseDto<ProductResponseDto> result = productService.getProducts(null, pageable);

        assertThat(result.content()).hasSize(1);
        assertThat(result.totalElements()).isEqualTo(1);
        verify(productRepository).findAll(pageable);
    }

    @Test
    @DisplayName("getProducts with search query should call searchProducts")
    void getProducts_WithSearch_ReturnsMatchingProducts() {
        Pageable pageable = PageRequest.of(0, 10);
        when(productRepository.searchProducts("mouse", pageable)).thenReturn(new PageImpl<>(List.of(sampleProduct)));

        PageResponseDto<ProductResponseDto> result = productService.getProducts("mouse", pageable);

        assertThat(result.content()).hasSize(1);
        verify(productRepository).searchProducts("mouse", pageable);
    }

    @Test
    @DisplayName("updateProduct should update and return modified product")
    void updateProduct_Success() {
        ProductRequestDto updateDto = new ProductRequestDto("Wireless Mouse Pro", "WM-001-PRO", "Updated",
                new BigDecimal("39.99"), 200, 30, null);

        when(productRepository.findById(1L)).thenReturn(Optional.of(sampleProduct));
        when(productRepository.existsBySkuIgnoreCaseAndIdNot("WM-001-PRO", 1L)).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenReturn(sampleProduct);

        ProductResponseDto result = productService.updateProduct(1L, updateDto);

        assertThat(result).isNotNull();
        verify(productRepository).save(sampleProduct);
    }

    @Test
    @DisplayName("updateProduct should throw DuplicateResourceException if updated SKU exists on another product")
    void updateProduct_DuplicateSku_ThrowsException() {
        ProductRequestDto updateDto = new ProductRequestDto("Wireless Mouse Pro", "EXISTING-SKU", "Updated",
                new BigDecimal("39.99"), 200, 30, null);

        when(productRepository.findById(1L)).thenReturn(Optional.of(sampleProduct));
        when(productRepository.existsBySkuIgnoreCaseAndIdNot("EXISTING-SKU", 1L)).thenReturn(true);

        assertThatThrownBy(() -> productService.updateProduct(1L, updateDto))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("EXISTING-SKU");

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("deleteProduct should delete when found")
    void deleteProduct_Success() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(sampleProduct));

        productService.deleteProduct(1L);

        verify(productRepository).delete(sampleProduct);
    }

    @Test
    @DisplayName("deleteProduct should throw ResourceNotFoundException when product does not exist")
    void deleteProduct_NotFound_ThrowsException() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.deleteProduct(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");

        verify(productRepository, never()).delete(any(Product.class));
    }
}
