package com.example.photoprintapplication1.repository;

import com.example.photoprintapplication1.models.DeviceLicense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeviceLicenseRepository extends JpaRepository<DeviceLicense, Long> {
    long countByLicenseId(Long licenseId);
    boolean existsByLicenseIdAndDeviceId(Long licenseId, Long deviceId);
}