package com.smartinventory.company.service;

import com.smartinventory.company.entity.Company;
import com.smartinventory.company.repository.CompanyRepository;
import com.smartinventory.security.service.CustomUserDetails;
import com.smartinventory.user.entity.User;
import com.smartinventory.user.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentCompanyProvider {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;

    public CurrentCompanyProvider() {
        this.userRepository = null;
        this.companyRepository = null;
    }

    public CurrentCompanyProvider(UserRepository userRepository, CompanyRepository companyRepository) {
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
    }

    public Company getCurrentCompany() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails userDetails) {
            if (userDetails.getCompany() != null) {
                return userDetails.getCompany();
            }
        }
        if (auth != null && auth.getName() != null && !auth.getName().isBlank() && userRepository != null) {
            User user = userRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase(auth.getName(), auth.getName())
                    .orElse(null);
            if (user != null && user.getCompany() != null) {
                return user.getCompany();
            }
        }
        // Fallback for bootstrap / test operations: default company (id 1)
        if (companyRepository == null) {
            Company defaultCompany = new Company("Smart Inventory Systems", "admin@sims.local", "+1 (555) 019-2834");
            defaultCompany.setId(1L);
            return defaultCompany;
        }
        return companyRepository.findById(1L).orElseGet(() -> {
            Company defaultCompany = new Company("Smart Inventory Systems", "admin@sims.local", "+1 (555) 019-2834");
            defaultCompany.setId(1L);
            return companyRepository.save(defaultCompany);
        });
    }

    public Long getCurrentCompanyId() {
        return getCurrentCompany().getId();
    }
}
