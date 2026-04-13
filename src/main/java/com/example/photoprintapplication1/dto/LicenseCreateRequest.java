package com.example.photoprintapplication1.dto;

public class LicenseCreateRequest {
    private Long productId;
    private Long typeId;
    private Long ownerId;       // владелец админ или компания
    private String description;

    // Геттеры и сеттеры
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public Long getTypeId() { return typeId; }
    public void setTypeId(Long typeId) { this.typeId = typeId; }

    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}