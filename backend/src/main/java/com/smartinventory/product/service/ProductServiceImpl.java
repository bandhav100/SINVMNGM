package com.smartinventory.product.service;

import com.smartinventory.category.entity.Category;
import com.smartinventory.category.repository.CategoryRepository;
import com.smartinventory.common.dto.PageResponseDto;
import com.smartinventory.common.exception.DuplicateResourceException;
import com.smartinventory.common.exception.ResourceNotFoundException;
import com.smartinventory.product.dto.ProductRequestDto;
import com.smartinventory.product.dto.ProductResponseDto;
import com.smartinventory.product.entity.Product;
import com.smartinventory.product.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductServiceImpl(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    @Override
    public ProductResponseDto createProduct(ProductRequestDto request) {
        String trimmedSku = request.sku().trim();
        if (productRepository.existsBySkuIgnoreCase(trimmedSku)) {
            throw new DuplicateResourceException("Product with SKU '" + trimmedSku + "' already exists");
        }

        Product product = new Product();
        product.setName(request.name().trim());
        product.setSku(trimmedSku);
        product.setDescription(request.description() != null ? request.description().trim() : null);
        product.setPrice(request.price());
        product.setQuantity(request.quantity());
        product.setReorderLevel(request.reorderLevel() != null ? request.reorderLevel() : 10);

        if (request.categoryId() != null) {
            Category category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + request.categoryId()));
            product.setCategory(category);
        }

        Product savedProduct = productRepository.save(product);
        return ProductResponseDto.fromEntity(savedProduct);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponseDto getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        return ProductResponseDto.fromEntity(product);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDto<ProductResponseDto> getProducts(String search, Pageable pageable) {
        Page<Product> page;
        if (search != null && !search.trim().isEmpty()) {
            page = productRepository.searchProducts(search.trim(), pageable);
        } else {
            page = productRepository.findAll(pageable);
        }
        return PageResponseDto.fromPage(page.map(ProductResponseDto::fromEntity));
    }

    @Override
    public ProductResponseDto updateProduct(Long id, ProductRequestDto request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        String trimmedSku = request.sku().trim();
        if (productRepository.existsBySkuIgnoreCaseAndIdNot(trimmedSku, id)) {
            throw new DuplicateResourceException("Product with SKU '" + trimmedSku + "' already exists on another product");
        }

        product.setName(request.name().trim());
        product.setSku(trimmedSku);
        product.setDescription(request.description() != null ? request.description().trim() : null);
        product.setPrice(request.price());
        product.setQuantity(request.quantity());
        if (request.reorderLevel() != null) {
            product.setReorderLevel(request.reorderLevel());
        }

        if (request.categoryId() != null) {
            Category category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + request.categoryId()));
            product.setCategory(category);
        } else {
            product.setCategory(null);
        }

        Product updatedProduct = productRepository.save(product);
        return ProductResponseDto.fromEntity(updatedProduct);
    }

    @Override
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        productRepository.delete(product);
    }
}
