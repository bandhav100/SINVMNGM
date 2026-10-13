package com.smartinventory.supplier.service;

import com.smartinventory.common.dto.PageResponseDto;
import com.smartinventory.common.exception.ResourceNotFoundException;
import com.smartinventory.company.entity.Company;
import com.smartinventory.company.service.CurrentCompanyProvider;
import com.smartinventory.supplier.dto.SupplierRequestDto;
import com.smartinventory.supplier.dto.SupplierResponseDto;
import com.smartinventory.supplier.entity.Supplier;
import com.smartinventory.supplier.repository.SupplierRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class SupplierServiceImpl implements SupplierService {

    private final SupplierRepository supplierRepository;
    private final CurrentCompanyProvider currentCompanyProvider;

    public SupplierServiceImpl(
            SupplierRepository supplierRepository,
            CurrentCompanyProvider currentCompanyProvider
    ) {
        this.supplierRepository = supplierRepository;
        this.currentCompanyProvider = currentCompanyProvider;
    }

    private Supplier findSupplierOrThrow(Long id) {
        if (currentCompanyProvider != null) {
            Long companyId = currentCompanyProvider.getCurrentCompanyId();
            return supplierRepository.findByIdAndCompanyId(id, companyId)
                    .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with id: " + id));
        }
        return supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with id: " + id));
    }

    @Override
    public SupplierResponseDto createSupplier(SupplierRequestDto request) {
        Company company = currentCompanyProvider != null ? currentCompanyProvider.getCurrentCompany() : null;

        Supplier supplier = new Supplier();
        supplier.setCompany(company);
        supplier.setName(request.name().trim());
        supplier.setContactPerson(request.contactPerson() != null ? request.contactPerson().trim() : null);
        supplier.setEmail(request.email() != null && !request.email().trim().isEmpty() ? request.email().trim() : null);
        supplier.setPhone(request.phone() != null ? request.phone().trim() : null);
        supplier.setAddress(request.address() != null ? request.address().trim() : null);
        supplier.setActive(request.active() != null ? request.active() : true);

        Supplier saved = supplierRepository.save(supplier);
        return SupplierResponseDto.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public SupplierResponseDto getSupplierById(Long id) {
        return SupplierResponseDto.fromEntity(findSupplierOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDto<SupplierResponseDto> getSuppliers(String search, Boolean active, Pageable pageable) {
        Page<Supplier> page;
        boolean hasSearch = search != null && !search.trim().isEmpty();

        if (currentCompanyProvider != null) {
            Long companyId = currentCompanyProvider.getCurrentCompanyId();
            if (hasSearch && active != null) {
                page = supplierRepository.searchSuppliersByActive(companyId, search.trim(), active, pageable);
            } else if (hasSearch) {
                page = supplierRepository.searchSuppliers(companyId, search.trim(), pageable);
            } else if (active != null) {
                page = supplierRepository.findByCompanyIdAndActive(companyId, active, pageable);
            } else {
                page = supplierRepository.findAllByCompanyId(companyId, pageable);
            }
        } else {
            if (hasSearch && active != null) {
                page = supplierRepository.searchSuppliersByActive(search.trim(), active, pageable);
            } else if (hasSearch) {
                page = supplierRepository.searchSuppliers(search.trim(), pageable);
            } else if (active != null) {
                page = supplierRepository.findByActive(active, pageable);
            } else {
                page = supplierRepository.findAll(pageable);
            }
        }

        return PageResponseDto.fromPage(page.map(SupplierResponseDto::fromEntity));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupplierResponseDto> getActiveSuppliers() {
        if (currentCompanyProvider != null) {
            Long companyId = currentCompanyProvider.getCurrentCompanyId();
            return supplierRepository.findByCompanyIdAndActiveTrue(companyId).stream()
                    .map(SupplierResponseDto::fromEntity)
                    .toList();
        }
        return supplierRepository.findByActiveTrue().stream()
                .map(SupplierResponseDto::fromEntity)
                .toList();
    }

    @Override
    public SupplierResponseDto updateSupplier(Long id, SupplierRequestDto request) {
        Supplier supplier = findSupplierOrThrow(id);

        supplier.setName(request.name().trim());
        supplier.setContactPerson(request.contactPerson() != null ? request.contactPerson().trim() : null);
        supplier.setEmail(request.email() != null && !request.email().trim().isEmpty() ? request.email().trim() : null);
        supplier.setPhone(request.phone() != null ? request.phone().trim() : null);
        supplier.setAddress(request.address() != null ? request.address().trim() : null);
        if (request.active() != null) {
            supplier.setActive(request.active());
        }

        Supplier updated = supplierRepository.save(supplier);
        return SupplierResponseDto.fromEntity(updated);
    }

    @Override
    public SupplierResponseDto toggleSupplierStatus(Long id, boolean active) {
        Supplier supplier = findSupplierOrThrow(id);
        supplier.setActive(active);
        Supplier updated = supplierRepository.save(supplier);
        return SupplierResponseDto.fromEntity(updated);
    }

    @Override
    public void deleteSupplier(Long id) {
        Supplier supplier = findSupplierOrThrow(id);
        supplierRepository.delete(supplier);
    }
}
