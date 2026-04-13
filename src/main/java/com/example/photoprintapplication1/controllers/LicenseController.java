package com.example.photoprintapplication1.controllers;

import com.example.photoprintapplication1.dto.ActivateLicenseRequest;
import com.example.photoprintapplication1.dto.LicenseCreateRequest;
import com.example.photoprintapplication1.dto.RenewLicenseRequest;
import com.example.photoprintapplication1.dto.CheckLicenseRequest;
import com.example.photoprintapplication1.dto.TicketResponse;
import com.example.photoprintapplication1.models.License;
import com.example.photoprintapplication1.models.User;
import com.example.photoprintapplication1.repository.UserRepository;
import com.example.photoprintapplication1.service.LicenseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/license")
public class LicenseController {

    @Autowired
    private LicenseService licenseService;

    @Autowired
    private UserRepository userRepository;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<License> createLicense(@RequestBody LicenseCreateRequest request,
                                                 Authentication authentication) {

        String username = authentication.getName();

        User admin = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Admin not found: " + username));

        Long adminId = admin.getId();   // ← Правильно берём ID пользователя

        License license = licenseService.createLicense(request, adminId);
        return ResponseEntity.status(201).body(license);
    }

    @PostMapping("/activate")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<TicketResponse> activateLicense(
            @RequestBody ActivateLicenseRequest request,
            Authentication authentication) {

        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        try {
            TicketResponse response = licenseService.activateLicense(request, user.getId());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/check")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<TicketResponse> checkLicense(@RequestBody CheckLicenseRequest request) {
        try {
            TicketResponse response = licenseService.checkLicense(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/renew")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<TicketResponse> renewLicense(
            @RequestBody RenewLicenseRequest request,
            Authentication authentication) {

        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        try {
            TicketResponse response = licenseService.renewLicense(request, user.getId());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}