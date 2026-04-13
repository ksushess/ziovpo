package com.example.photoprintapplication1.repository;

import com.example.photoprintapplication1.models.LicenseHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LicenseHistoryRepository extends JpaRepository<LicenseHistory, Long> {
}