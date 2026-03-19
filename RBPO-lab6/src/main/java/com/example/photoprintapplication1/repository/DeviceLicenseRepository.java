package com.example.photoprintapplication1.repository;

import com.example.photoprintapplication1.models.DeviceLicense;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceLicenseRepository extends JpaRepository<DeviceLicense, Long> {
}