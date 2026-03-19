package com.example.photoprintapplication1.models;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "license")
public class License {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "type_id", nullable = false)
    private LicenseType type;

    private LocalDateTime firstActivationDate;
    private LocalDateTime endingDate;

    private boolean blocked = false;

    private int deviceCount = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

    private String description;

    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "license", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DeviceLicense> deviceLicenses = new ArrayList<>();

    @OneToMany(mappedBy = "license", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LicenseHistory> history = new ArrayList<>();

    // Конструкторы
    public License() {
    }

    // Геттеры и сеттеры (все вручную)
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public LicenseType getType() {
        return type;
    }

    public void setType(LicenseType type) {
        this.type = type;
    }

    public LocalDateTime getFirstActivationDate() {
        return firstActivationDate;
    }

    public void setFirstActivationDate(LocalDateTime firstActivationDate) {
        this.firstActivationDate = firstActivationDate;
    }

    public LocalDateTime getEndingDate() {
        return endingDate;
    }

    public void setEndingDate(LocalDateTime endingDate) {
        this.endingDate = endingDate;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }

    public int getDeviceCount() {
        return deviceCount;
    }

    public void setDeviceCount(int deviceCount) {
        this.deviceCount = deviceCount;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<DeviceLicense> getDeviceLicenses() {
        return deviceLicenses;
    }

    public void setDeviceLicenses(List<DeviceLicense> deviceLicenses) {
        this.deviceLicenses = deviceLicenses;
    }

    public List<LicenseHistory> getHistory() {
        return history;
    }

    public void setHistory(List<LicenseHistory> history) {
        this.history = history;
    }

    public void addDeviceLicense(DeviceLicense dl) {
        deviceLicenses.add(dl);
        dl.setLicense(this);
    }

    public void addHistory(LicenseHistory h) {
        history.add(h);
        h.setLicense(this);
    }

    @Override
    public String toString() {
        return "License{" +
                "id=" + id +
                ", code='" + code + '\'' +
                ", userId=" + (user != null ? user.getId() : null) +
                ", productId=" + (product != null ? product.getId() : null) +
                ", typeId=" + (type != null ? type.getId() : null) +
                ", firstActivationDate=" + firstActivationDate +
                ", endingDate=" + endingDate +
                ", blocked=" + blocked +
                ", deviceCount=" + deviceCount +
                ", ownerId=" + (owner != null ? owner.getId() : null) +
                ", createdAt=" + createdAt +
                '}';
    }
}