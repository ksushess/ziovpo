package com.example.photoprintapplication1.dto;

public class RenewLicenseRequest {
    private String code;  // код лицензии, которую продлеваем

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}