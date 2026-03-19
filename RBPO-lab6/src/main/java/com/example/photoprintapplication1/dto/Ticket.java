package com.example.photoprintapplication1.dto;

import java.time.Duration;
import java.time.LocalDateTime;

public class Ticket {
    private LocalDateTime currentDate;
    private Duration ticketLifetime;
    private LocalDateTime activationDate;
    private LocalDateTime expirationDate;
    private Long userId;
    private Long deviceId;
    private boolean blocked;

    // Конструктор по умолчанию
    public Ticket() {
    }

    // Геттеры и сеттеры (все вручную, без Lombok)
    public LocalDateTime getCurrentDate() {
        return currentDate;
    }

    public void setCurrentDate(LocalDateTime currentDate) {
        this.currentDate = currentDate;
    }

    public Duration getTicketLifetime() {
        return ticketLifetime;
    }

    public void setTicketLifetime(Duration ticketLifetime) {
        this.ticketLifetime = ticketLifetime;
    }

    public LocalDateTime getActivationDate() {
        return activationDate;
    }

    public void setActivationDate(LocalDateTime activationDate) {
        this.activationDate = activationDate;
    }

    public LocalDateTime getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(LocalDateTime expirationDate) {
        this.expirationDate = expirationDate;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }
}