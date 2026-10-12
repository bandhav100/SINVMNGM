package com.smartinventory.purchase.service.impl;

import com.smartinventory.common.dto.PageResponseDto;
import com.smartinventory.common.exception.InvalidStateTransitionException;
import com.smartinventory.common.exception.ResourceNotFoundException;
import com.smartinventory.inventory.entity.InventoryTransaction;
import com.smartinventory.inventory.model.InventoryTransactionType;
import com.smartinventory.inventory.repository.InventoryTransactionRepository;
import com.smartinventory.product.entity.Product;
import com.smartinventory.product.repository.ProductRepository;
import com.smartinventory.purchase.dto.*;
import com.smartinventory.purchase.entity.PurchaseOrder;
import com.smartinventory.purchase.entity.PurchaseOrderItem;
import com.smartinventory.purchase.model.PurchaseOrderStatus;
import com.smartinventory.purchase.repository.PurchaseOrderRepository;
import com.smartinventory.purchase.service.PurchaseService;
import com.smartinventory.supplier.entity.Supplier;
import com.smartinventory.supplier.repository.SupplierRepository;
import com.smartinventory.user.entity.User;
import com.smartinventory.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PurchaseServiceImpl implements PurchaseService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;

    public PurchaseServiceImpl(
            PurchaseOrderRepository purchaseOrderRepository,
            SupplierRepository supplierRepository,
            ProductRepository productRepository,
            UserRepository userRepository,
            InventoryTransactionRepository inventoryTransactionRepository
    ) {
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.supplierRepository = supplierRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.inventoryTransactionRepository = inventoryTransactionRepository;
    }

    @Override
    @Transactional
    public PurchaseOrderResponseDto createPurchaseOrder(CreatePurchaseOrderRequestDto request, String currentUsername) {
        User user = findUserByUsername(currentUsername);

        Supplier supplier = supplierRepository.findById(request.supplierId())
                .orElseThrow(() -> new ResourceNotFoundException("Supplier", "id", request.supplierId()));

        if (!supplier.isActive()) {
            throw new IllegalArgumentException("Cannot create purchase order for inactive supplier: " + supplier.getName());
        }

        validateItems(request.items());

        PurchaseOrder order = new PurchaseOrder();
        order.setOrderNumber(generateOrderNumber());
        order.setSupplier(supplier);
        order.setExpectedDeliveryDate(request.expectedDeliveryDate());
        order.setNotes(request.notes());
        order.setCreatedBy(user);

        populateAndCalculateItems(order, request.items());

        if (request.status() == PurchaseOrderStatus.ORDERED) {
            order.setStatus(PurchaseOrderStatus.ORDERED);
            order.setOrderedAt(Instant.now());
        } else {
            order.setStatus(PurchaseOrderStatus.DRAFT);
        }

        PurchaseOrder savedOrder = purchaseOrderRepository.save(order);
        return PurchaseOrderResponseDto.fromEntity(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDto<PurchaseOrderSummaryDto> getPurchaseOrders(
            String search,
            PurchaseOrderStatus status,
            Long supplierId,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable
    ) {
        Page<PurchaseOrder> page = purchaseOrderRepository.searchPurchaseOrders(
                search != null ? search.trim() : null,
                status,
                supplierId,
                startDate,
                endDate,
                pageable
        );
        return PageResponseDto.fromPage(page, PurchaseOrderSummaryDto::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public PurchaseOrderResponseDto getPurchaseOrderById(Long id) {
        PurchaseOrder order = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PurchaseOrder", "id", id));
        return PurchaseOrderResponseDto.fromEntity(order);
    }

    @Override
    @Transactional
    public PurchaseOrderResponseDto updatePurchaseOrder(Long id, UpdatePurchaseOrderRequestDto request, String currentUsername) {
        PurchaseOrder order = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PurchaseOrder", "id", id));

        if (order.getStatus() != PurchaseOrderStatus.DRAFT) {
            throw new InvalidStateTransitionException("Only DRAFT purchase orders can be updated. Current status is " + order.getStatus());
        }

        Supplier supplier = supplierRepository.findById(request.supplierId())
                .orElseThrow(() -> new ResourceNotFoundException("Supplier", "id", request.supplierId()));

        if (!supplier.isActive()) {
            throw new IllegalArgumentException("Cannot update purchase order with inactive supplier: " + supplier.getName());
        }

        validateItems(request.items());

        order.setSupplier(supplier);
        order.setExpectedDeliveryDate(request.expectedDeliveryDate());
        order.setNotes(request.notes());

        order.clearItems();
        purchaseOrderRepository.flush();
        populateAndCalculateItems(order, request.items());

        PurchaseOrder updatedOrder = purchaseOrderRepository.save(order);
        return PurchaseOrderResponseDto.fromEntity(updatedOrder);
    }

    @Override
    @Transactional
    public void deletePurchaseOrder(Long id) {
        PurchaseOrder order = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PurchaseOrder", "id", id));

        if (order.getStatus() != PurchaseOrderStatus.DRAFT) {
            throw new InvalidStateTransitionException("Only DRAFT purchase orders can be deleted. Current status is " + order.getStatus());
        }

        purchaseOrderRepository.delete(order);
    }

    @Override
    @Transactional
    public PurchaseOrderResponseDto submitPurchaseOrder(Long id, String currentUsername) {
        PurchaseOrder order = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PurchaseOrder", "id", id));

        if (order.getStatus() != PurchaseOrderStatus.DRAFT) {
            throw new InvalidStateTransitionException("Only DRAFT purchase orders can be submitted as ORDERED. Current status is " + order.getStatus());
        }

        if (order.getItems().isEmpty()) {
            throw new IllegalArgumentException("Cannot submit an empty purchase order");
        }

        order.setStatus(PurchaseOrderStatus.ORDERED);
        order.setOrderedAt(Instant.now());

        PurchaseOrder updatedOrder = purchaseOrderRepository.save(order);
        return PurchaseOrderResponseDto.fromEntity(updatedOrder);
    }

    @Override
    @Transactional
    public PurchaseOrderResponseDto receivePurchaseOrder(Long id, String currentUsername) {
        // Concurrency-safe: lock the purchase order with pessimistic write lock
        PurchaseOrder order = purchaseOrderRepository.findByIdWithLock(id)
                .orElseThrow(() -> new ResourceNotFoundException("PurchaseOrder", "id", id));

        if (order.getStatus() == PurchaseOrderStatus.RECEIVED) {
            throw new InvalidStateTransitionException("Purchase order " + order.getOrderNumber() + " has already been received.");
        }

        if (order.getStatus() != PurchaseOrderStatus.ORDERED) {
            throw new InvalidStateTransitionException("Cannot receive purchase order in " + order.getStatus() + " status. Order must be in ORDERED status.");
        }

        if (order.getItems().isEmpty()) {
            throw new IllegalArgumentException("Purchase order contains no items to receive");
        }

        User receivingUser = findUserByUsername(currentUsername);

        // Sort product IDs in ascending order and acquire pessimistic write locks to prevent deadlocks
        List<Long> productIds = order.getItems().stream()
                .map(item -> item.getProduct().getId())
                .sorted()
                .toList();

        List<Product> products = productRepository.findAllByIdInOrderByIdAscForUpdate(productIds);
        Map<Long, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        for (PurchaseOrderItem item : order.getItems()) {
            Product product = productMap.get(item.getProduct().getId());
            if (product == null) {
                throw new ResourceNotFoundException("Product", "id", item.getProduct().getId());
            }

            // Check ledger idempotency: prevent duplicate receiving entries for this PO and product
            if (inventoryTransactionRepository.existsByReferenceTypeAndReferenceIdAndProductId("PURCHASE_ORDER", order.getId(), product.getId())) {
                throw new InvalidStateTransitionException("Stock for product " + product.getSku() + " has already been received for purchase order " + order.getOrderNumber());
            }

            int before = product.getQuantity();
            int delta = item.getQuantity();
            int after = before + delta;

            InventoryTransaction txn = new InventoryTransaction();
            txn.setProduct(product);
            txn.setTransactionType(InventoryTransactionType.PURCHASE_RECEIPT);
            txn.setQuantityDelta(delta);
            txn.setQuantityBefore(before);
            txn.setQuantityAfter(after);
            txn.setReferenceType("PURCHASE_ORDER");
            txn.setReferenceId(order.getId());
            txn.setPerformedBy(receivingUser);
            txn.setNotes("Received via PO " + order.getOrderNumber());

            inventoryTransactionRepository.save(txn);

            product.setQuantity(after);
            productRepository.save(product);
        }

        order.setStatus(PurchaseOrderStatus.RECEIVED);
        order.setReceivedAt(Instant.now());
        order.setReceivedBy(receivingUser);

        PurchaseOrder receivedOrder = purchaseOrderRepository.save(order);
        return PurchaseOrderResponseDto.fromEntity(receivedOrder);
    }

    @Override
    @Transactional
    public PurchaseOrderResponseDto cancelPurchaseOrder(Long id, String currentUsername) {
        PurchaseOrder order = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PurchaseOrder", "id", id));

        if (order.getStatus() == PurchaseOrderStatus.RECEIVED) {
            throw new InvalidStateTransitionException("Cannot cancel a purchase order that has already been RECEIVED.");
        }

        if (order.getStatus() == PurchaseOrderStatus.CANCELLED) {
            throw new InvalidStateTransitionException("Purchase order is already CANCELLED.");
        }

        order.setStatus(PurchaseOrderStatus.CANCELLED);
        PurchaseOrder cancelledOrder = purchaseOrderRepository.save(order);
        return PurchaseOrderResponseDto.fromEntity(cancelledOrder);
    }

    private void validateItems(List<PurchaseOrderItemRequestDto> items) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Purchase order must contain at least one item");
        }

        Set<Long> productIds = new HashSet<>();
        for (PurchaseOrderItemRequestDto item : items) {
            if (!productIds.add(item.productId())) {
                throw new IllegalArgumentException("Duplicate product ID found in purchase order items: " + item.productId());
            }
        }
    }

    private void populateAndCalculateItems(PurchaseOrder order, List<PurchaseOrderItemRequestDto> itemDtos) {
        Set<Long> productIds = itemDtos.stream().map(PurchaseOrderItemRequestDto::productId).collect(Collectors.toSet());
        List<Product> products = productRepository.findAllById(productIds);
        Map<Long, Product> productMap = products.stream().collect(Collectors.toMap(Product::getId, p -> p));

        BigDecimal orderTotal = BigDecimal.ZERO;

        for (PurchaseOrderItemRequestDto itemDto : itemDtos) {
            Product product = productMap.get(itemDto.productId());
            if (product == null) {
                throw new ResourceNotFoundException("Product", "id", itemDto.productId());
            }

            BigDecimal unitCost = itemDto.unitCost().setScale(2, RoundingMode.HALF_UP);
            BigDecimal totalCost = unitCost.multiply(BigDecimal.valueOf(itemDto.quantity())).setScale(2, RoundingMode.HALF_UP);

            PurchaseOrderItem item = new PurchaseOrderItem();
            item.setProduct(product);
            item.setQuantity(itemDto.quantity());
            item.setUnitCost(unitCost);
            item.setTotalCost(totalCost);

            order.addItem(item);
            orderTotal = orderTotal.add(totalCost);
        }

        order.setTotalAmount(orderTotal.setScale(2, RoundingMode.HALF_UP));
    }

    private String generateOrderNumber() {
        int year = LocalDate.now().getYear();
        String randomSuffix = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        String orderNumber = "PO-" + year + "-" + randomSuffix;

        while (purchaseOrderRepository.existsByOrderNumber(orderNumber)) {
            randomSuffix = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
            orderNumber = "PO-" + year + "-" + randomSuffix;
        }

        return orderNumber;
    }

    private User findUserByUsername(String username) {
        if (username == null || username.isBlank()) {
            return null;
        }
        return userRepository.findByUsernameIgnoreCase(username).orElse(null);
    }
}
