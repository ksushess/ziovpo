package com.example.photoprintapplication1.dto;

public class Ticket {

    private String currentDate;
    private String ticketLifetime;
    private String activationDate;
    private String expirationDate;
    private Long userId;
    private Long deviceId;
    private boolean blocked;

    public Ticket() {
    }

    public String getCurrentDate() { return currentDate; }
    public void setCurrentDate(String currentDate) { this.currentDate = currentDate; }

    public String getTicketLifetime() { return ticketLifetime; }
    public void setTicketLifetime(String ticketLifetime) { this.ticketLifetime = ticketLifetime; }

    public String getActivationDate() { return activationDate; }
    public void setActivationDate(String activationDate) { this.activationDate = activationDate; }

    public String getExpirationDate() { return expirationDate; }
    public void setExpirationDate(String expirationDate) { this.expirationDate = expirationDate; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getDeviceId() { return deviceId; }
    public void setDeviceId(Long deviceId) { this.deviceId = deviceId; }

    public boolean isBlocked() { return blocked; }
    public void setBlocked(boolean blocked) { this.blocked = blocked; }
}