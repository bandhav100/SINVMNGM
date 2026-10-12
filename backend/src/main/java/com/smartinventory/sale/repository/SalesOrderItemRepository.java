package com.smartinventory.sale.repository;

import com.smartinventory.sale.entity.SalesOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SalesOrderItemRepository extends JpaRepository<SalesOrderItem, Long> {

    List<SalesOrderItem> findBySalesOrderId(Long salesOrderId);
}
