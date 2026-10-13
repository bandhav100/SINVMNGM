package com.smartinventory.product.service;

import com.smartinventory.category.entity.Category;
import com.smartinventory.category.repository.CategoryRepository;
import com.smartinventory.common.dto.PageResponseDto;
import com.smartinventory.common.exception.DuplicateResourceException;
import com.smartinventory.common.exception.ResourceNotFoundException;
import com.smartinventory.company.entity.Company;
import com.smartinventory.company.service.CurrentCompanyProvider;
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
    private final CurrentCompanyProvider currentCompanyProvider;

    public ProductServiceImpl(
            ProductRepository productRepository,
            CategoryRepository categoryRepository,
            CurrentCompanyProvider currentCompanyProvider
    ) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.currentCompanyProvider = currentCompanyProvider;
    }

    private Product findProductOrThrow(Long id) {
        if (currentCompanyProvider != null && currentCompanyProvider.getCurrentCompanyId() != null) {
            return productRepository.findByIdAndCompanyId(id, currentCompanyProvider.getCurrentCompanyId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        }
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
    }

    @Override
    public ProductResponseDto createProduct(ProductRequestDto request) {
        String trimmedSku = request.sku().trim();

        if (currentCompanyProvider != null && currentCompanyProvider.getCurrentCompanyId() != null) {
            Long companyId = currentCompanyProvider.getCurrentCompanyId();
            if (productRepository.existsByCompanyIdAndSkuIgnoreCase(companyId, trimmedSku)) {
                throw new DuplicateResourceException("Product with SKU '" + trimmedSku + "' already exists");
            }
        } else {
            if (productRepository.existsBySkuIgnoreCase(trimmedSku)) {
                throw new DuplicateResourceException("Product with SKU '" + trimmedSku + "' already exists");
            }
        }

        Company company = (currentCompanyProvider != null) ? currentCompanyProvider.getCurrentCompany() : null;

        Product product = new Product();
        product.setCompany(company);
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
        return ProductResponseDto.fromEntity(findProductOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDto<ProductResponseDto> getProducts(String search, Pageable pageable) {
        Page<Product> page;
        boolean hasSearch = (search != null && !search.trim().isEmpty());

        if (currentCompanyProvider != null && currentCompanyProvider.getCurrentCompanyId() != null) {
            Long companyId = currentCompanyProvider.getCurrentCompanyId();
            if (hasSearch) {
                page = productRepository.searchProducts(companyId, search.trim(), pageable);
            } else {
                page = productRepository.findAllByCompanyId(companyId, pageable);
            }
        } else {
            if (hasSearch) {
                page = productRepository.searchProducts(search.trim(), pageable);
            } else {
                page = productRepository.findAll(pageable);
            }
        }

        return PageResponseDto.fromPage(page.map(ProductResponseDto::fromEntity));
    }

    @Override
    public ProductResponseDto updateProduct(Long id, ProductRequestDto request) {
        Product product = findProductOrThrow(id);
        String trimmedSku = request.sku().trim();

        if (currentCompanyProvider != null && currentCompanyProvider.getCurrentCompanyId() != null) {
            Long companyId = currentCompanyProvider.getCurrentCompanyId();
            if (productRepository.existsByCompanyIdAndSkuIgnoreCaseAndIdNot(companyId, trimmedSku, id)) {
                throw new DuplicateResourceException("Product with SKU '" + trimmedSku + "' already exists on another product");
            }
        } else {
            if (productRepository.existsBySkuIgnoreCaseAndIdNot(trimmedSku, id)) {
                throw new DuplicateResourceException("Product with SKU '" + trimmedSku + "' already exists on another product");
            }
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

        Product saved = productRepository.save(product);
        return ProductResponseDto.fromEntity(saved);
    }

    @Override
    public void deleteProduct(Long id) {
        Product product = findProductOrThrow(id);
        productRepository.delete(product);
    }
}
