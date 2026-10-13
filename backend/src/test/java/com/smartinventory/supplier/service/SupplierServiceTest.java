package com.smartinventory.supplier.service;

import com.smartinventory.common.dto.PageResponseDto;
import com.smartinventory.common.exception.ResourceNotFoundException;
import com.smartinventory.supplier.dto.SupplierRequestDto;
import com.smartinventory.supplier.dto.SupplierResponseDto;
import com.smartinventory.supplier.entity.Supplier;
import com.smartinventory.supplier.repository.SupplierRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SupplierServiceTest {

    @Mock
    private SupplierRepository supplierRepository;

    @InjectMocks
    private SupplierServiceImpl supplierService;

    private Supplier sampleSupplier;
    private SupplierRequestDto sampleRequest;

    @BeforeEach
    void setUp() {
        sampleSupplier = new Supplier(1L, "Global Logistics Ltd", "John Smith", "john@global.com", "+1-555-0199", "123 Port Road", true);
        sampleSupplier.setCreatedAt(Instant.now());
        sampleSupplier.setUpdatedAt(Instant.now());

        sampleRequest = new SupplierRequestDto("Global Logistics Ltd", "John Smith", "john@global.com", "+1-555-0199", "123 Port Road", true);
    }

    @Test
    @DisplayName("createSupplier should persist and return supplier DTO")
    void createSupplier_Success() {
        when(supplierRepository.save(any(Supplier.class))).thenReturn(sampleSupplier);

        SupplierResponseDto response = supplierService.createSupplier(sampleRequest);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Global Logistics Ltd");
        assertThat(response.active()).isTrue();
        verify(supplierRepository).save(any(Supplier.class));
    }

    @Test
    @DisplayName("getSupplierById should return supplier when found")
    void getSupplierById_Success() {
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(sampleSupplier));

        SupplierResponseDto response = supplierService.getSupplierById(1L);

        assertThat(response).isNotNull();
        assertThat(response.email()).isEqualTo("john@global.com");
    }

    @Test
    @DisplayName("getSupplierById should throw ResourceNotFoundException when not found")
    void getSupplierById_NotFound_ThrowsException() {
        when(supplierRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> supplierService.getSupplierById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    @DisplayName("getSuppliers should return paginated results")
    void getSuppliers_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        when(supplierRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(sampleSupplier)));

        PageResponseDto<SupplierResponseDto> response = supplierService.getSuppliers(null, null, pageable);

        assertThat(response.content()).hasSize(1);
        assertThat(response.totalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("toggleSupplierStatus should toggle active state")
    void toggleSupplierStatus_Success() {
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(sampleSupplier));
        when(supplierRepository.save(any(Supplier.class))).thenReturn(sampleSupplier);

        SupplierResponseDto response = supplierService.toggleSupplierStatus(1L, false);

        assertThat(response).isNotNull();
        verify(supplierRepository).save(sampleSupplier);
    }

    @Test
    @DisplayName("deleteSupplier should delete entity when found")
    void deleteSupplier_Success() {
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(sampleSupplier));

        supplierService.deleteSupplier(1L);

        verify(supplierRepository).delete(sampleSupplier);
    }
}
