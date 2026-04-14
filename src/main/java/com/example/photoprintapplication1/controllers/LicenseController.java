package com.example.photoprintapplication1.controllers;

import com.example.photoprintapplication1.dto.*;
import com.example.photoprintapplication1.models.License;
import com.example.photoprintapplication1.models.User;
import com.example.photoprintapplication1.repository.UserRepository;
import com.example.photoprintapplication1.service.LicenseService;
import com.example.photoprintapplication1.service.SignatureService;
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

    @Autowired
    private SignatureService signatureService;

    // Создание лицензии (только админ)
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<License> createLicense(@RequestBody LicenseCreateRequest request,
                                                 Authentication authentication) {

        String username = authentication.getName();

        User admin = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Admin not found: " + username));

        Long adminId = admin.getId();

        License license = licenseService.createLicense(request, adminId);
        return ResponseEntity.status(201).body(license);
    }

    // Активация лицензии
    @PostMapping("/activate")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<TicketResponse> activateLicense(
            @RequestBody ActivateLicenseRequest request,
            Authentication authentication) {

        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        try {
            TicketResponse response = licenseService.activateLicense(request, user.getId());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new TicketResponse(null, "Error: " + e.getMessage()));
        }
    }

    // Проверка лицензии
    @PostMapping("/check")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<TicketResponse> checkLicense(@RequestBody CheckLicenseRequest request) {
        try {
            TicketResponse response = licenseService.checkLicense(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new TicketResponse(null, "Error: " + e.getMessage()));
        }
    }

    // Продление лицензии
    @PostMapping("/renew")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<TicketResponse> renewLicense(
            @RequestBody RenewLicenseRequest request,
            Authentication authentication) {

        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        try {
            TicketResponse response = licenseService.renewLicense(request, user.getId());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new TicketResponse(null, "Error: " + e.getMessage()));
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<String> verifySignature(@RequestBody TicketResponse response) {
        try {
            boolean isValid = signatureService.verifyTicket(response.getTicket(), response.getSignature());
            if (isValid) {
                return ResponseEntity.ok("Подпись верна");
            } else {
                return ResponseEntity.badRequest().body("Подпись НЕ верна");
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Ошибка проверки подписи: " + e.getMessage());
        }
    }
}