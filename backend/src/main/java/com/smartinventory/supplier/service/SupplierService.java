package com.smartinventory.supplier.service;

import com.smartinventory.common.dto.PageResponseDto;
import com.smartinventory.supplier.dto.SupplierRequestDto;
import com.smartinventory.supplier.dto.SupplierResponseDto;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface SupplierService {
    SupplierResponseDto createSupplier(SupplierRequestDto request);
    SupplierResponseDto getSupplierById(Long id);
    PageResponseDto<SupplierResponseDto> getSuppliers(String search, Boolean active, Pageable pageable);
    List<SupplierResponseDto> getActiveSuppliers();
    SupplierResponseDto updateSupplier(Long id, SupplierRequestDto request);
    SupplierResponseDto toggleSupplierStatus(Long id, boolean active);
    void deleteSupplier(Long id);
}
