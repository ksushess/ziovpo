package com.example.photoprintapplication1.repository;

import com.example.photoprintapplication1.models.License;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface LicenseRepository extends JpaRepository<License, Long> {
    Optional<License> findByCode(String code);
    Optional<License> findFirstByDeviceLicensesDeviceIdOrderByEndingDateDesc(Long deviceId);
}