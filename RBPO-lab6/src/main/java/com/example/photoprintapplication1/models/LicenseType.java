package com.example.photoprintapplication1.models;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "license_type")
public class LicenseType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(name = "default_duration_in_days", nullable = false)
    private Integer defaultDurationInDays;

    private String description;

    @OneToMany(mappedBy = "type", cascade = CascadeType.ALL)
    private List<License> licenses = new ArrayList<>();

    public LicenseType() {}

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getDefaultDurationInDays() { return defaultDurationInDays; }
    public void setDefaultDurationInDays(Integer defaultDurationInDays) { this.defaultDurationInDays = defaultDurationInDays; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<License> getLicenses() { return licenses; }
    public void setLicenses(List<License> licenses) { this.licenses = licenses; }

    public void addLicense(License license) {
        licenses.add(license);
        license.setType(this);
    }
}