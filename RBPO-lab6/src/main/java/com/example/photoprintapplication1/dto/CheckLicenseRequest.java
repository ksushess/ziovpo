package com.example.photoprintapplication1.dto;

public class CheckLicenseRequest {

    private String deviceMac;

    public CheckLicenseRequest() {
    }

    public String getDeviceMac() {
        return deviceMac;
    }

    public void setDeviceMac(String deviceMac) {
        this.deviceMac = deviceMac;
    }
}