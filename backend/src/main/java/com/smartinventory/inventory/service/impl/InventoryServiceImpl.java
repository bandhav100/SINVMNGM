package com.smartinventory.inventory.service.impl;

import com.smartinventory.common.dto.PageResponseDto;
import com.smartinventory.common.exception.ResourceNotFoundException;
import com.smartinventory.inventory.dto.*;
import com.smartinventory.inventory.entity.InventoryTransaction;
import com.smartinventory.inventory.entity.StockAdjustment;
import com.smartinventory.inventory.model.InventoryTransactionType;
import com.smartinventory.inventory.repository.InventoryTransactionRepository;
import com.smartinventory.inventory.repository.StockAdjustmentRepository;
import com.smartinventory.inventory.service.InventoryService;
import com.smartinventory.product.entity.Product;
import com.smartinventory.product.repository.ProductRepository;
import com.smartinventory.user.entity.User;
import com.smartinventory.user.repository.UserRepository;
import com.smartinventory.company.service.CurrentCompanyProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class InventoryServiceImpl implements InventoryService {

    private final ProductRepository productRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final StockAdjustmentRepository stockAdjustmentRepository;
    private final UserRepository userRepository;
    private final CurrentCompanyProvider currentCompanyProvider;

    @org.springframework.beans.factory.annotation.Autowired
    public InventoryServiceImpl(
            ProductRepository productRepository,
            InventoryTransactionRepository inventoryTransactionRepository,
            StockAdjustmentRepository stockAdjustmentRepository,
            UserRepository userRepository,
            CurrentCompanyProvider currentCompanyProvider
    ) {
        this.productRepository = productRepository;
        this.inventoryTransactionRepository = inventoryTransactionRepository;
        this.stockAdjustmentRepository = stockAdjustmentRepository;
        this.userRepository = userRepository;
        this.currentCompanyProvider = currentCompanyProvider;
    }

    public InventoryServiceImpl(
            ProductRepository productRepository,
            InventoryTransactionRepository inventoryTransactionRepository,
            StockAdjustmentRepository stockAdjustmentRepository,
            UserRepository userRepository
    ) {
        this(productRepository, inventoryTransactionRepository, stockAdjustmentRepository, userRepository, null);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDto<InventoryStockItemDto> getInventoryStock(
            Long categoryId,
            String status,
            String search,
            Pageable pageable
    ) {
        Page<Product> page = (currentCompanyProvider != null && currentCompanyProvider.getCurrentCompanyId() != null)
                ? productRepository.searchInventoryReport(currentCompanyProvider.getCurrentCompanyId(), categoryId, status, search, pageable)
                : productRepository.searchInventoryReport(categoryId, status, search, pageable);
        return PageResponseDto.fromPage(page, this::mapToStockItemDto);
    }

    @Override
    @Transactional(readOnly = true)
    public InventorySummaryDto getInventorySummary() {
        long totalProducts;
        long totalStockUnits;
        long outOfStockCount;
        long lowStockCount;
        long reorderRequiredCount;
        BigDecimal totalRetailValue;

        if (currentCompanyProvider != null && currentCompanyProvider.getCurrentCompanyId() != null) {
            Long companyId = currentCompanyProvider.getCurrentCompanyId();
            totalProducts = productRepository.countTotalProducts(companyId);
            totalStockUnits = productRepository.sumTotalStockUnits(companyId);
            outOfStockCount = productRepository.countOutOfStock(companyId);
            lowStockCount = productRepository.countLowStock(companyId);
            reorderRequiredCount = productRepository.countReorderRequired(companyId);
            totalRetailValue = productRepository.sumTotalRetailValue(companyId);
        } else {
            totalProducts = productRepository.countTotalProducts();
            totalStockUnits = productRepository.sumTotalStockUnits();
            outOfStockCount = productRepository.countOutOfStock();
            lowStockCount = productRepository.countLowStock();
            reorderRequiredCount = productRepository.countReorderRequired();
            totalRetailValue = productRepository.sumTotalRetailValue();
        }

        long inStockCount = Math.max(0, totalProducts - reorderRequiredCount);
        if (totalRetailValue == null) {
            totalRetailValue = BigDecimal.ZERO;
        }

        return new InventorySummaryDto(
                totalProducts,
                totalStockUnits,
                inStockCount,
                lowStockCount,
                outOfStockCount,
                reorderRequiredCount,
                totalRetailValue
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDto<InventoryTransactionDto> getTransactions(
            Long productId,
            InventoryTransactionType transactionType,
            String search,
            Pageable pageable
    ) {
        Page<InventoryTransaction> page = (currentCompanyProvider != null && currentCompanyProvider.getCurrentCompanyId() != null)
                ? inventoryTransactionRepository.searchTransactions(currentCompanyProvider.getCurrentCompanyId(), productId, transactionType, search, pageable)
                : inventoryTransactionRepository.searchTransactions(productId, transactionType, search, pageable);
        return PageResponseDto.fromPage(page, this::mapToTransactionDto);
    }

    @Override
    @Transactional
    public StockAdjustmentResponseDto adjustStock(StockAdjustmentRequestDto request, String username) {
        if (request.quantityDelta() == null || request.quantityDelta() == 0) {
            throw new IllegalArgumentException("Stock adjustment quantity delta cannot be zero");
        }
        if (request.reason() == null || request.reason().trim().isEmpty()) {
            throw new IllegalArgumentException("Reason for stock adjustment is mandatory");
        }

        // Lock product with pessimistic write lock scoped by company if available
        Product product = (currentCompanyProvider != null && currentCompanyProvider.getCurrentCompanyId() != null)
                ? productRepository.findByIdAndCompanyIdWithLock(request.productId(), currentCompanyProvider.getCurrentCompanyId())
                        .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + request.productId()))
                : productRepository.findByIdWithLock(request.productId())
                        .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + request.productId()));

        int before = product.getQuantity();
        int after = before + request.quantityDelta();

        if (after < 0) {
            throw new com.smartinventory.common.exception.InsufficientStockException(
                    "Insufficient stock: adjustment of " + request.quantityDelta()
                    + " would reduce stock below zero (current stock: " + before + ")");
        }

        User user = null;
        if (username != null) {
            user = userRepository.findByUsernameIgnoreCase(username).orElse(null);
        }

        // 1. Create StockAdjustment audit record
        StockAdjustment adjustment = new StockAdjustment();
        if (currentCompanyProvider != null) {
            adjustment.setCompany(currentCompanyProvider.getCurrentCompany());
        }
        adjustment.setProduct(product);
        adjustment.setQuantityDelta(request.quantityDelta());
        adjustment.setQuantityBefore(before);
        adjustment.setQuantityAfter(after);
        adjustment.setReason(request.reason().trim());
        adjustment.setNotes(request.notes() != null ? request.notes().trim() : null);
        adjustment.setAdjustedBy(user);
        adjustment = stockAdjustmentRepository.save(adjustment);

        // 2. Create InventoryTransaction ledger entry
        String noteText = "Reason: " + adjustment.getReason()
                + (adjustment.getNotes() != null && !adjustment.getNotes().isEmpty() ? " • " + adjustment.getNotes() : "");
        InventoryTransaction tx = new InventoryTransaction();
        if (currentCompanyProvider != null) {
            tx.setCompany(currentCompanyProvider.getCurrentCompany());
        }
        tx.setProduct(product);
        tx.setTransactionType(InventoryTransactionType.MANUAL_ADJUSTMENT);
        tx.setQuantityDelta(request.quantityDelta());
        tx.setQuantityBefore(before);
        tx.setQuantityAfter(after);
        tx.setReferenceType("STOCK_ADJUSTMENT");
        tx.setReferenceId(adjustment.getId());
        tx.setPerformedBy(user);
        tx.setNotes(noteText);
        inventoryTransactionRepository.save(tx);

        // 3. Atomically update product quantity
        product.setQuantity(after);
        productRepository.save(product);

        return mapToAdjustmentResponseDto(adjustment);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDto<StockAdjustmentResponseDto> getAdjustments(Long productId, Pageable pageable) {
        Page<StockAdjustment> page = (currentCompanyProvider != null && currentCompanyProvider.getCurrentCompanyId() != null)
                ? stockAdjustmentRepository.findByProductIdOptional(currentCompanyProvider.getCurrentCompanyId(), productId, pageable)
                : stockAdjustmentRepository.findByProductIdOptional(productId, pageable);
        return PageResponseDto.fromPage(page, this::mapToAdjustmentResponseDto);
    }

    private InventoryStockItemDto mapToStockItemDto(Product product) {
        String stockStatus;
        if (product.getQuantity() <= 0) {
            stockStatus = "OUT_OF_STOCK";
        } else if (product.getQuantity() <= product.getReorderLevel()) {
            stockStatus = "LOW_STOCK";
        } else {
            stockStatus = "IN_STOCK";
        }

        BigDecimal retailVal = product.getPrice().multiply(BigDecimal.valueOf(Math.max(0, product.getQuantity())));
        String catName = product.getCategory() != null ? product.getCategory().getName() : "Uncategorized";
        Long catId = product.getCategory() != null ? product.getCategory().getId() : null;

        return new InventoryStockItemDto(
                product.getId(),
                product.getName(),
                product.getSku(),
                catId,
                catName,
                product.getPrice(),
                product.getQuantity(),
                product.getReorderLevel(),
                stockStatus,
                retailVal
        );
    }

    private InventoryTransactionDto mapToTransactionDto(InventoryTransaction tx) {
        String performedBy = tx.getPerformedBy() != null ? tx.getPerformedBy().getUsername() : "System";
        String prodName = tx.getProduct() != null ? tx.getProduct().getName() : "Unknown Product";
        String sku = tx.getProduct() != null ? tx.getProduct().getSku() : "";

        return new InventoryTransactionDto(
                tx.getId(),
                tx.getProduct() != null ? tx.getProduct().getId() : null,
                prodName,
                sku,
                tx.getTransactionType(),
                tx.getQuantityDelta(),
                tx.getQuantityBefore(),
                tx.getQuantityAfter(),
                tx.getReferenceType(),
                tx.getReferenceId(),
                performedBy,
                tx.getNotes(),
                tx.getCreatedAt()
        );
    }

    private StockAdjustmentResponseDto mapToAdjustmentResponseDto(StockAdjustment sa) {
        String adjustedBy = sa.getAdjustedBy() != null ? sa.getAdjustedBy().getUsername() : "System";
        String prodName = sa.getProduct() != null ? sa.getProduct().getName() : "Unknown Product";
        String sku = sa.getProduct() != null ? sa.getProduct().getSku() : "";

        return new StockAdjustmentResponseDto(
                sa.getId(),
                sa.getProduct() != null ? sa.getProduct().getId() : null,
                prodName,
                sku,
                sa.getQuantityDelta(),
                sa.getQuantityBefore(),
                sa.getQuantityAfter(),
                sa.getReason(),
                sa.getNotes(),
                adjustedBy,
                sa.getCreatedAt()
        );
    }
}
