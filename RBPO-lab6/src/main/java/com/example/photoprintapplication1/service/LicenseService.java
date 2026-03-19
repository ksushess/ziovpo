package com.example.photoprintapplication1.service;

import com.example.photoprintapplication1.dto.ActivateLicenseRequest;
import com.example.photoprintapplication1.dto.LicenseCreateRequest;
import com.example.photoprintapplication1.dto.Ticket;
import com.example.photoprintapplication1.models.*;
import com.example.photoprintapplication1.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;
import com.example.photoprintapplication1.dto.CheckLicenseRequest;
import com.example.photoprintapplication1.dto.TicketResponse;
import com.example.photoprintapplication1.dto.RenewLicenseRequest;
import com.example.photoprintapplication1.dto.CheckLicenseRequest;
import com.example.photoprintapplication1.dto.ActivateLicenseRequest;
import com.example.photoprintapplication1.service.SignatureService;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class LicenseService {

    @Autowired private LicenseRepository licenseRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private LicenseTypeRepository licenseTypeRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private LicenseHistoryRepository historyRepository;
    @Autowired private DeviceRepository deviceRepository;
    @Autowired private DeviceLicenseRepository deviceLicenseRepository;
    @Autowired private SignatureService signatureService;  // ← добавлен сервис подписи

    @Transactional
    public License createLicense(LicenseCreateRequest request, Long adminId) {
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found"));

        LicenseType type = licenseTypeRepository.findById(request.getTypeId())
                .orElseThrow(() -> new RuntimeException("License type not found"));

        User owner = admin;
        if (request.getOwnerId() != null) {
            owner = userRepository.findById(request.getOwnerId())
                    .orElseThrow(() -> new RuntimeException("Owner not found"));
        }

        License license = new License();
        license.setCode(generateUniqueCode());
        license.setProduct(product);
        license.setType(type);
        license.setOwner(owner);
        license.setUser(null);
        license.setFirstActivationDate(null);
        license.setEndingDate(null);
        license.setBlocked(false);
        license.setDeviceCount(0);
        license.setDescription(request.getDescription());

        license = licenseRepository.save(license);

        LicenseHistory history = new LicenseHistory();
        history.setLicense(license);
        history.setUser(admin);
        history.setStatus("CREATED");
        history.setDescription("Лицензия создана администратором");
        historyRepository.save(history);

        return license;
    }

    @Transactional
    public TicketResponse activateLicense(ActivateLicenseRequest request, Long userId) throws Exception {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        License license = licenseRepository.findByCode(request.getActivationKey())
                .orElseThrow(() -> new RuntimeException("License not found"));

        if (license.getUser() != null) {
            throw new RuntimeException("License already activated");
        }

        Device device = deviceRepository.findByMacAddress(request.getDeviceMac())
                .orElseGet(() -> {
                    Device newDevice = new Device();
                    newDevice.setName(request.getDeviceName());
                    newDevice.setMacAddress(request.getDeviceMac());
                    newDevice.setUser(user);
                    return deviceRepository.save(newDevice);
                });

        if (license.getDeviceCount() >= 5) {
            throw new RuntimeException("Device limit reached");
        }

        license.setUser(user);
        license.setFirstActivationDate(LocalDateTime.now());
        license.setEndingDate(LocalDateTime.now().plusDays(license.getType().getDefaultDurationInDays()));
        license.setDeviceCount(license.getDeviceCount() + 1);

        DeviceLicense dl = new DeviceLicense();
        dl.setLicense(license);
        dl.setDevice(device);
        dl.setActivationDate(LocalDateTime.now());
        deviceLicenseRepository.save(dl);

        license.getDeviceLicenses().add(dl);

        licenseRepository.save(license);

        LicenseHistory history = new LicenseHistory();
        history.setLicense(license);
        history.setUser(user);
        history.setStatus("ACTIVATED");
        history.setDescription("Лицензия активирована на устройстве " + device.getMacAddress());
        historyRepository.save(history);

        Ticket ticket = buildTicket(license);
        String signature = signatureService.signTicket(ticket);

        return new TicketResponse(ticket, signature);
    }

    @Transactional(readOnly = true)
    public TicketResponse checkLicense(CheckLicenseRequest request) throws Exception {
        Device device = deviceRepository.findByMacAddress(request.getDeviceMac())
                .orElseThrow(() -> new RuntimeException("Device not found"));

        License license = licenseRepository.findByDeviceLicensesDeviceId(device.getId())
                .orElseThrow(() -> new RuntimeException("License not found"));

        if (license.isBlocked()) {
            throw new RuntimeException("License is blocked");
        }

        if (license.getEndingDate() == null || license.getEndingDate().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("License expired");
        }

        if (!license.getUser().getId().equals(device.getUser().getId())) {
            throw new RuntimeException("License not bound to this user");
        }

        Ticket ticket = buildTicket(license);
        String signature = signatureService.signTicket(ticket);

        return new TicketResponse(ticket, signature);
    }

    @Transactional
    public TicketResponse renewLicense(RenewLicenseRequest request, Long userId) throws Exception {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        License license = licenseRepository.findByCode(request.getCode())
                .orElseThrow(() -> new RuntimeException("License not found"));

        if (!license.getUser().getId().equals(userId) && !user.getRole().equals("ADMIN")) {
            throw new RuntimeException("Not allowed to renew this license");
        }

        if (license.getEndingDate() == null || license.getEndingDate().isAfter(LocalDateTime.now().plusDays(7))) {
            throw new RuntimeException("Renewal not allowed (more than 7 days left or not activated)");
        }

        long daysToAdd = license.getType().getDefaultDurationInDays();
        license.setEndingDate(license.getEndingDate().plusDays(daysToAdd));

        licenseRepository.save(license);

        LicenseHistory history = new LicenseHistory();
        history.setLicense(license);
        history.setUser(user);
        history.setStatus("RENEWED");
        history.setDescription("Лицензия продлена на " + daysToAdd + " дней пользователем " + user.getUsername());
        historyRepository.save(history);

        Ticket ticket = buildTicket(license);
        String signature = signatureService.signTicket(ticket);

        return new TicketResponse(ticket, signature);
    }

    private Ticket buildTicket(License license) {
        Ticket ticket = new Ticket();
        ticket.setCurrentDate(LocalDateTime.now());
        ticket.setTicketLifetime(Duration.ofHours(24));
        ticket.setActivationDate(license.getFirstActivationDate());
        ticket.setExpirationDate(license.getEndingDate());
        ticket.setUserId(license.getUser().getId());
        ticket.setDeviceId(license.getDeviceLicenses().get(0).getDevice().getId());
        ticket.setBlocked(license.isBlocked());
        return ticket;
    }

    private String generateUniqueCode() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }
}