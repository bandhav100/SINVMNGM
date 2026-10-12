package com.smartinventory.product.service;

import com.smartinventory.common.dto.PageResponseDto;
import com.smartinventory.product.dto.ProductRequestDto;
import com.smartinventory.product.dto.ProductResponseDto;
import org.springframework.data.domain.Pageable;

public interface ProductService {
    ProductResponseDto createProduct(ProductRequestDto request);
    ProductResponseDto getProductById(Long id);
    PageResponseDto<ProductResponseDto> getProducts(String search, Pageable pageable);
    ProductResponseDto updateProduct(Long id, ProductRequestDto request);
    void deleteProduct(Long id);
}
