package com.smartinventory.settings.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateSystemSettingsDto(
        @NotBlank(message = "Company name is required")
        @Size(max = 100, message = "Company name must not exceed 100 characters")
        String companyName,

        @Email(message = "Invalid company email format")
        @Size(max = 100, message = "Company email must not exceed 100 characters")
        String companyEmail,

        @Size(max = 50, message = "Company phone must not exceed 50 characters")
        String companyPhone,

        @NotBlank(message = "Currency code is required")
        @Size(max = 10, message = "Currency code must not exceed 10 characters")
        String currency,

        @NotBlank(message = "Timezone is required")
        @Size(max = 50, message = "Timezone must not exceed 50 characters")
        String timezone,

        @NotNull(message = "Default reorder threshold is required")
        @Min(value = 0, message = "Default reorder threshold must be 0 or greater")
        Integer defaultReorderThreshold,

        @NotNull(message = "Low-stock alert flag is required")
        Boolean lowStockAlertsEnabled,

        @NotNull(message = "Out-of-stock alert flag is required")
        Boolean outOfStockAlertsEnabled,

        @NotNull(message = "Negative stock flag is required")
        Boolean allowNegativeStock,

        @NotNull(message = "Purchase approval flag is required")
        Boolean requirePurchaseApproval
) {}
