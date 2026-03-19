package com.example.photoprintapplication1.controllers;

import com.example.photoprintapplication1.dto.*;
import com.example.photoprintapplication1.models.License;
import com.example.photoprintapplication1.models.User;
import com.example.photoprintapplication1.service.LicenseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/license")
public class LicenseController {

    @Autowired
    private LicenseService licenseService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<License> createLicense(
            @RequestBody LicenseCreateRequest request,
            Authentication authentication) {
        // Предполагаем, что authentication.getName() возвращает строку с ID
        Long adminId = Long.valueOf(authentication.getName());
        License license = licenseService.createLicense(request, adminId);
        return ResponseEntity.status(201).body(license);
    }

    @PostMapping("/activate")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<TicketResponse> activateLicense(
            @RequestBody ActivateLicenseRequest request,
            @AuthenticationPrincipal User user) throws Exception {

        TicketResponse response = licenseService.activateLicense(request, user.getId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/check")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<TicketResponse> checkLicense(
            @RequestBody CheckLicenseRequest request) throws Exception {

        TicketResponse response = licenseService.checkLicense(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/renew")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<TicketResponse> renewLicense(
            @RequestBody RenewLicenseRequest request,
            @AuthenticationPrincipal User user) throws Exception {

        TicketResponse response = licenseService.renewLicense(request, user.getId());
        return ResponseEntity.ok(response);
    }
}