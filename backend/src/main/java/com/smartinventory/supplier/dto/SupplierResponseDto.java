package com.smartinventory.supplier.dto;

import com.smartinventory.supplier.entity.Supplier;
import java.time.Instant;

public record SupplierResponseDto(
    Long id,
    String name,
    String contactPerson,
    String email,
    String phone,
    String address,
    Boolean active,
    Instant createdAt,
    Instant updatedAt
) {
    public static SupplierResponseDto fromEntity(Supplier supplier) {
        return new SupplierResponseDto(
            supplier.getId(),
            supplier.getName(),
            supplier.getContactPerson(),
            supplier.getEmail(),
            supplier.getPhone(),
            supplier.getAddress(),
            supplier.getActive(),
            supplier.getCreatedAt(),
            supplier.getUpdatedAt()
        );
    }
}
