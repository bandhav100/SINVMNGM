package com.smartinventory.sale.service.impl;

import com.smartinventory.common.dto.PageResponseDto;
import com.smartinventory.common.exception.InsufficientStockException;
import com.smartinventory.common.exception.InvalidStateTransitionException;
import com.smartinventory.common.exception.ResourceNotFoundException;
import com.smartinventory.company.entity.Company;
import com.smartinventory.company.service.CurrentCompanyProvider;
import com.smartinventory.inventory.entity.InventoryTransaction;
import com.smartinventory.inventory.model.InventoryTransactionType;
import com.smartinventory.inventory.repository.InventoryTransactionRepository;
import com.smartinventory.product.entity.Product;
import com.smartinventory.product.repository.ProductRepository;
import com.smartinventory.sale.dto.*;
import com.smartinventory.sale.entity.SalesOrder;
import com.smartinventory.sale.entity.SalesOrderItem;
import com.smartinventory.sale.model.SalesOrderStatus;
import com.smartinventory.sale.repository.SalesOrderRepository;
import com.smartinventory.sale.service.SaleService;
import com.smartinventory.user.entity.User;
import com.smartinventory.user.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SaleServiceImpl implements SaleService {

    private final SalesOrderRepository salesOrderRepository;
    private final ProductRepository productRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final UserRepository userRepository;
    private final CurrentCompanyProvider currentCompanyProvider;

    public SaleServiceImpl(
            SalesOrderRepository salesOrderRepository,
            ProductRepository productRepository,
            InventoryTransactionRepository inventoryTransactionRepository,
            UserRepository userRepository,
            CurrentCompanyProvider currentCompanyProvider
    ) {
        this.salesOrderRepository = salesOrderRepository;
        this.productRepository = productRepository;
        this.inventoryTransactionRepository = inventoryTransactionRepository;
        this.userRepository = userRepository;
        this.currentCompanyProvider = currentCompanyProvider;
    }

    private SalesOrder findOrderOrThrow(Long id) {
        if (currentCompanyProvider != null && currentCompanyProvider.getCurrentCompanyId() != null) {
            return salesOrderRepository.findByIdAndCompanyId(id, currentCompanyProvider.getCurrentCompanyId())
                    .orElseThrow(() -> new ResourceNotFoundException("SalesOrder", "id", id));
        }
        return salesOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SalesOrder", "id", id));
    }

    @Override
    @Transactional
    public SalesOrderResponseDto createSaleOrder(CreateSalesOrderRequestDto request, String currentUsername) {
        validateItems(request.items());

        User user = findUserByUsername(currentUsername);

        SalesOrder order = new SalesOrder();
        Company company = currentCompanyProvider != null ? currentCompanyProvider.getCurrentCompany() : null;
        order.setCompany(company);
        order.setOrderNumber(generateOrderNumber());
        order.setCustomerName(request.customerName());
        order.setCustomerContact(request.customerContact());
        order.setNotes(request.notes());
        order.setCreatedBy(user);
        order.setStatus(SalesOrderStatus.DRAFT);

        populateAndCalculateItems(order, request.items());

        SalesOrder savedOrder = salesOrderRepository.save(order);
        return SalesOrderResponseDto.fromEntity(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDto<SalesOrderSummaryDto> getSalesOrders(
            String search,
            SalesOrderStatus status,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable
    ) {
        Specification<SalesOrder> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (currentCompanyProvider != null && currentCompanyProvider.getCurrentCompanyId() != null) {
                predicates.add(cb.equal(root.get("company").get("id"), currentCompanyProvider.getCurrentCompanyId()));
            }

            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("orderNumber")), pattern),
                        cb.like(cb.lower(root.get("customerName")), pattern)
                ));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startDate.atStartOfDay().toInstant(java.time.ZoneOffset.UTC)));
            }

            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), endDate.plusDays(1).atStartOfDay().toInstant(java.time.ZoneOffset.UTC)));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<SalesOrder> page = salesOrderRepository.findAll(spec, pageable);
        return PageResponseDto.fromPage(page, SalesOrderSummaryDto::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public SalesOrderResponseDto getSaleOrderById(Long id) {
        return SalesOrderResponseDto.fromEntity(findOrderOrThrow(id));
    }

    @Override
    @Transactional
    public SalesOrderResponseDto updateSaleOrder(Long id, UpdateSalesOrderRequestDto request, String currentUsername) {
        SalesOrder order = findOrderOrThrow(id);

        if (order.getStatus() != SalesOrderStatus.DRAFT) {
            throw new InvalidStateTransitionException("Only DRAFT sales orders can be updated. Current status is " + order.getStatus());
        }

        validateItems(request.items());

        order.setCustomerName(request.customerName());
        order.setCustomerContact(request.customerContact());
        order.setNotes(request.notes());

        order.clearItems();
        salesOrderRepository.flush();
        populateAndCalculateItems(order, request.items());

        SalesOrder updatedOrder = salesOrderRepository.save(order);
        return SalesOrderResponseDto.fromEntity(updatedOrder);
    }

    @Override
    @Transactional
    public void deleteSaleOrder(Long id) {
        SalesOrder order = findOrderOrThrow(id);

        if (order.getStatus() != SalesOrderStatus.DRAFT) {
            throw new InvalidStateTransitionException("Only DRAFT sales orders can be deleted. Current status is " + order.getStatus());
        }

        salesOrderRepository.delete(order);
    }

    @Override
    @Transactional
    public SalesOrderResponseDto completeSaleOrder(Long id, String currentUsername) {
        // 1. Lock the sales order with pessimistic write lock
        SalesOrder order;
        if (currentCompanyProvider != null && currentCompanyProvider.getCurrentCompanyId() != null) {
            order = salesOrderRepository.findByIdAndCompanyIdWithLock(id, currentCompanyProvider.getCurrentCompanyId())
                    .orElseThrow(() -> new ResourceNotFoundException("SalesOrder", "id", id));
        } else {
            order = salesOrderRepository.findByIdWithLock(id)
                    .orElseThrow(() -> new ResourceNotFoundException("SalesOrder", "id", id));
        }

        // 2. Validate current state
        if (order.getStatus() == SalesOrderStatus.COMPLETED) {
            throw new InvalidStateTransitionException("Sales order " + order.getOrderNumber() + " has already been completed.");
        }
        if (order.getStatus() == SalesOrderStatus.CANCELLED) {
            throw new InvalidStateTransitionException("Cannot complete a CANCELLED sales order.");
        }
        if (order.getStatus() != SalesOrderStatus.DRAFT) {
            throw new InvalidStateTransitionException("Cannot complete sales order in " + order.getStatus() + " status. Order must be in DRAFT status.");
        }

        if (order.getItems().isEmpty()) {
            throw new IllegalArgumentException("Cannot complete an empty sales order");
        }

        User completingUser = findUserByUsername(currentUsername);

        // 3. Acquire locks on products in ascending ID order to prevent deadlocks
        List<Long> productIds = order.getItems().stream()
                .map(item -> item.getProduct().getId())
                .sorted()
                .toList();

        List<Product> lockedProducts = productRepository.findAllByIdInOrderByIdAscForUpdate(productIds);
        Map<Long, Product> productMap = lockedProducts.stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        // 4. Pre-validate stock availability for all items before any deduction
        for (SalesOrderItem item : order.getItems()) {
            Product product = productMap.get(item.getProduct().getId());
            if (product == null) {
                throw new ResourceNotFoundException("Product", "id", item.getProduct().getId());
            }

            if (product.getQuantity() < item.getQuantity()) {
                throw new InsufficientStockException(
                        "Insufficient stock for product '" + product.getName() + "' (SKU: " + product.getSku() + "). " +
                        "Available: " + product.getQuantity() + ", requested: " + item.getQuantity()
                );
            }
        }

        // 5. Atomic stock deduction and inventory ledger entry creation
        for (SalesOrderItem item : order.getItems()) {
            Product product = productMap.get(item.getProduct().getId());
            int before = product.getQuantity();
            int after = before - item.getQuantity();

            InventoryTransaction txn = new InventoryTransaction();
            Company company = currentCompanyProvider != null ? currentCompanyProvider.getCurrentCompany() : null;
            txn.setCompany(company);
            txn.setProduct(product);
            txn.setTransactionType(InventoryTransactionType.SALE_DEDUCTION);
            txn.setQuantityDelta(-item.getQuantity());
            txn.setQuantityBefore(before);
            txn.setQuantityAfter(after);
            txn.setReferenceType("SALES_ORDER");
            txn.setReferenceId(order.getId());
            txn.setPerformedBy(completingUser);
            txn.setNotes("Sale order completed: " + order.getOrderNumber());

            inventoryTransactionRepository.save(txn);

            product.setQuantity(after);
            productRepository.save(product);
        }

        // 6. Transition order status to COMPLETED
        order.setStatus(SalesOrderStatus.COMPLETED);
        order.setCompletedAt(Instant.now());
        order.setCompletedBy(completingUser);

        SalesOrder completedOrder = salesOrderRepository.save(order);
        return SalesOrderResponseDto.fromEntity(completedOrder);
    }

    @Override
    @Transactional
    public SalesOrderResponseDto cancelSaleOrder(Long id, String currentUsername) {
        SalesOrder order = findOrderOrThrow(id);

        if (order.getStatus() == SalesOrderStatus.COMPLETED) {
            throw new InvalidStateTransitionException("Cannot cancel a sales order that has already been COMPLETED.");
        }

        if (order.getStatus() == SalesOrderStatus.CANCELLED) {
            throw new InvalidStateTransitionException("Sales order is already CANCELLED.");
        }

        order.setStatus(SalesOrderStatus.CANCELLED);
        SalesOrder cancelledOrder = salesOrderRepository.save(order);
        return SalesOrderResponseDto.fromEntity(cancelledOrder);
    }

    private void validateItems(List<SalesOrderItemRequestDto> items) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Sales order must contain at least one item");
        }

        Set<Long> productIds = new HashSet<>();
        for (SalesOrderItemRequestDto item : items) {
            if (item.quantity() == null || item.quantity() <= 0) {
                throw new IllegalArgumentException("Quantity must be greater than 0");
            }
            if (item.unitPrice() != null && item.unitPrice().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Unit price cannot be negative");
            }
            if (!productIds.add(item.productId())) {
                throw new IllegalArgumentException("Duplicate product ID found in sales order items: " + item.productId());
            }
        }
    }

    private void populateAndCalculateItems(SalesOrder order, List<SalesOrderItemRequestDto> itemDtos) {
        Set<Long> productIds = itemDtos.stream()
                .map(SalesOrderItemRequestDto::productId)
                .collect(Collectors.toSet());
        List<Product> products = productRepository.findAllById(productIds);
        Map<Long, Product> productMap = products.stream()
                .filter(p -> p.getCompany() == null || currentCompanyProvider == null || currentCompanyProvider.getCurrentCompanyId() == null || p.getCompany().getId().equals(currentCompanyProvider.getCurrentCompanyId()))
                .collect(Collectors.toMap(Product::getId, p -> p));

        BigDecimal orderTotal = BigDecimal.ZERO;

        for (SalesOrderItemRequestDto itemDto : itemDtos) {
            Product product = productMap.get(itemDto.productId());
            if (product == null) {
                throw new ResourceNotFoundException("Product", "id", itemDto.productId());
            }

            // Use server-side product price if unitPrice not explicitly specified
            BigDecimal unitPrice = itemDto.unitPrice() != null
                    ? itemDto.unitPrice().setScale(2, RoundingMode.HALF_UP)
                    : product.getPrice().setScale(2, RoundingMode.HALF_UP);

            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(itemDto.quantity()))
                    .setScale(2, RoundingMode.HALF_UP);

            SalesOrderItem item = new SalesOrderItem();
            item.setProduct(product);
            item.setQuantity(itemDto.quantity());
            item.setUnitPrice(unitPrice);
            item.setLineTotal(lineTotal);

            order.addItem(item);
            orderTotal = orderTotal.add(lineTotal);
        }

        order.setTotalAmount(orderTotal.setScale(2, RoundingMode.HALF_UP));
    }

    private String generateOrderNumber() {
        int year = LocalDate.now().getYear();
        String randomSuffix = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        String orderNumber = "SO-" + year + "-" + randomSuffix;

        if (currentCompanyProvider != null && currentCompanyProvider.getCurrentCompanyId() != null) {
            while (salesOrderRepository.existsByOrderNumberAndCompanyId(orderNumber, currentCompanyProvider.getCurrentCompanyId())) {
                randomSuffix = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
                orderNumber = "SO-" + year + "-" + randomSuffix;
            }
        } else {
            while (salesOrderRepository.existsByOrderNumber(orderNumber)) {
                randomSuffix = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
                orderNumber = "SO-" + year + "-" + randomSuffix;
            }
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
