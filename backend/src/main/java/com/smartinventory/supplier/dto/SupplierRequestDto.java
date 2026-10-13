package com.smartinventory.supplier.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SupplierRequestDto(
    @NotBlank(message = "Supplier company name is required")
    @Size(max = 255, message = "Supplier name cannot exceed 255 characters")
    String name,

    @Size(max = 150, message = "Contact person cannot exceed 150 characters")
    String contactPerson,

    @Email(message = "Email must be a valid email address")
    @Size(max = 255, message = "Email cannot exceed 255 characters")
    String email,

    @Size(max = 50, message = "Phone cannot exceed 50 characters")
    String phone,

    @Size(max = 1000, message = "Address cannot exceed 1000 characters")
    String address,

    Boolean active
) {}
