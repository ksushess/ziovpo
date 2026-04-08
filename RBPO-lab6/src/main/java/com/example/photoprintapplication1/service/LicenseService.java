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
import com.example.photoprintapplication1.repository.DeviceLicenseRepository;

import java.time.LocalDateTime;
import java.time.Duration;
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

        // Проверка: лицензия не должна быть уже активирована другим пользователем
        if (license.getUser() != null && !license.getUser().getId().equals(userId)) {
            throw new RuntimeException("License already activated by another user");
        }

        // Если это первая активация — проверяем, что пользователь является владельцем или админом
        if (license.getUser() == null && !license.getOwner().getId().equals(userId) && !"ADMIN".equals(user.getRole())) {
            throw new RuntimeException("Only owner or admin can activate this license");
        }

        Device device = deviceRepository.findByMacAddress(request.getDeviceMac())
                .orElseGet(() -> {
                    Device d = new Device();
                    d.setName(request.getDeviceName());
                    d.setMacAddress(request.getDeviceMac());
                    d.setUser(user);
                    return deviceRepository.save(d);
                });
        // есть ли уже связь этой лицензии с этим устройством
        boolean alreadyActivatedOnThisDevice = deviceLicenseRepository.existsByLicenseIdAndDeviceId(license.getId(), device.getId());
        if (alreadyActivatedOnThisDevice) {
            throw new RuntimeException("License already activated on this device");
        }

        // Лимит устройств
        long currentCount = deviceLicenseRepository.countByLicenseId(license.getId());
        if (currentCount >= 5) {
            throw new RuntimeException("Device limit reached");
        }

        // Активация
        if (license.getUser() == null) {  // первая активация
            license.setUser(user);
            license.setFirstActivationDate(LocalDateTime.now());
            license.setEndingDate(LocalDateTime.now().plusDays(license.getType().getDefaultDurationInDays()));
        }

        license.setDeviceCount((int) currentCount + 1);

        license = licenseRepository.save(license);

        // Связь устройство-лицензия
        DeviceLicense dl = new DeviceLicense();
        dl.setLicense(license);
        dl.setDevice(device);
        dl.setActivationDate(LocalDateTime.now());
        deviceLicenseRepository.save(dl);

        license.getDeviceLicenses().add(dl);

        // История
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
        // Находим устройство по MAC
        Device device = deviceRepository.findByMacAddress(request.getDeviceMac())
                .orElseThrow(() -> new RuntimeException("Device not found with MAC: " + request.getDeviceMac()));

        // Ищем самую актуальную (не просроченную) лицензию для этого устройства
        License license = licenseRepository.findFirstByDeviceLicensesDeviceIdOrderByEndingDateDesc(device.getId())
                .orElseThrow(() -> new RuntimeException("No active license found for this device"));

        // Проверки согласно логике задания
        if (license.isBlocked()) {
            throw new RuntimeException("License is blocked");
        }

        if (license.getEndingDate() == null || license.getEndingDate().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("License has expired");
        }

        // Важная проверка: лицензия должна быть привязана к пользователю устройства
        if (license.getUser() == null) {
            throw new RuntimeException("License is not activated yet");
        }

        if (!license.getUser().getId().equals(device.getUser().getId())) {
            throw new RuntimeException("License not bound to this user");
        }

        // Формируем тикет и подпись
        Ticket ticket = buildTicket(license);
        String signature = signatureService.signTicket(ticket);

        return new TicketResponse(ticket, signature);
    }

    @Transactional
    public TicketResponse renewLicense(RenewLicenseRequest request, Long userId) throws Exception {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        License license = licenseRepository.findByCode(request.getCode())
                .orElseThrow(() -> new RuntimeException("License not found with code: " + request.getCode()));

        // Проверка прав
        if (!license.getUser().getId().equals(userId) && !"ADMIN".equals(user.getRole())) {
            throw new RuntimeException("Not allowed to renew this license");
        }

        // продлевать можно, если осталось не больше 7 дней
        if (license.getEndingDate() == null || license.getEndingDate().isAfter(LocalDateTime.now().plusDays(7))) {
            throw new RuntimeException("Renewal not allowed (more than 7 days left)");
        }

        long daysToAdd = license.getType().getDefaultDurationInDays();

        license.setEndingDate(license.getEndingDate().plusDays(daysToAdd));
        licenseRepository.save(license);

        LicenseHistory history = new LicenseHistory();
        history.setLicense(license);
        history.setUser(user);
        history.setStatus("RENEWED");
        history.setDescription("Лицензия продлена на " + daysToAdd + " дней");
        historyRepository.save(history);

        Ticket ticket = buildTicket(license);
        String signature = signatureService.signTicket(ticket);

        return new TicketResponse(ticket, signature);
    }

    private Ticket buildTicket(License license) {
        Ticket ticket = new Ticket();
        ticket.setCurrentDate(LocalDateTime.now().toString());
        ticket.setTicketLifetime("24h");
        ticket.setActivationDate(license.getFirstActivationDate() != null ? license.getFirstActivationDate().toString() : null);
        ticket.setExpirationDate(license.getEndingDate() != null ? license.getEndingDate().toString() : null);
        ticket.setUserId(license.getUser() != null ? license.getUser().getId() : null);
        ticket.setDeviceId(!license.getDeviceLicenses().isEmpty() ? license.getDeviceLicenses().get(0).getDevice().getId() : null);
        ticket.setBlocked(license.isBlocked());
        return ticket;
    }

    private String generateUniqueCode() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }
}