package com.smartinventory.settings.entity;

import com.smartinventory.user.entity.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "system_settings")
public class SystemSettings {

    @Id
    private Long id = 1L;

    @Column(name = "company_name", nullable = false, length = 100)
    private String companyName;

    @Column(name = "company_email", length = 100)
    private String companyEmail;

    @Column(name = "company_phone", length = 50)
    private String companyPhone;

    @Column(name = "currency", nullable = false, length = 10)
    private String currency = "INR";

    @Column(name = "timezone", nullable = false, length = 50)
    private String timezone = "UTC";

    @Column(name = "default_reorder_threshold", nullable = false)
    private Integer defaultReorderThreshold = 10;

    @Column(name = "low_stock_alerts_enabled", nullable = false)
    private Boolean lowStockAlertsEnabled = true;

    @Column(name = "out_of_stock_alerts_enabled", nullable = false)
    private Boolean outOfStockAlertsEnabled = true;

    @Column(name = "allow_negative_stock", nullable = false)
    private Boolean allowNegativeStock = false;

    @Column(name = "require_purchase_approval", nullable = false)
    private Boolean requirePurchaseApproval = false;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private User updatedBy;

    public SystemSettings() {
    }

    @PrePersist
    @PreUpdate
    protected void onSave() {
        this.updatedAt = Instant.now();
        if (this.id == null) {
            this.id = 1L;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getCompanyEmail() {
        return companyEmail;
    }

    public void setCompanyEmail(String companyEmail) {
        this.companyEmail = companyEmail;
    }

    public String getCompanyPhone() {
        return companyPhone;
    }

    public void setCompanyPhone(String companyPhone) {
        this.companyPhone = companyPhone;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public Integer getDefaultReorderThreshold() {
        return defaultReorderThreshold;
    }

    public void setDefaultReorderThreshold(Integer defaultReorderThreshold) {
        this.defaultReorderThreshold = defaultReorderThreshold;
    }

    public Boolean getLowStockAlertsEnabled() {
        return lowStockAlertsEnabled;
    }

    public void setLowStockAlertsEnabled(Boolean lowStockAlertsEnabled) {
        this.lowStockAlertsEnabled = lowStockAlertsEnabled;
    }

    public Boolean getOutOfStockAlertsEnabled() {
        return outOfStockAlertsEnabled;
    }

    public void setOutOfStockAlertsEnabled(Boolean outOfStockAlertsEnabled) {
        this.outOfStockAlertsEnabled = outOfStockAlertsEnabled;
    }

    public Boolean getAllowNegativeStock() {
        return allowNegativeStock;
    }

    public void setAllowNegativeStock(Boolean allowNegativeStock) {
        this.allowNegativeStock = allowNegativeStock;
    }

    public Boolean getRequirePurchaseApproval() {
        return requirePurchaseApproval;
    }

    public void setRequirePurchaseApproval(Boolean requirePurchaseApproval) {
        this.requirePurchaseApproval = requirePurchaseApproval;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public User getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(User updatedBy) {
        this.updatedBy = updatedBy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SystemSettings that = (SystemSettings) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
