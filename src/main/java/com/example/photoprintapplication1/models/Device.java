package com.example.photoprintapplication1.models;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "device")
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "mac_address", unique = true, nullable = false)
    private String macAddress;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "device", cascade = CascadeType.ALL)
    private List<DeviceLicense> deviceLicenses = new ArrayList<>();

    public Device() {}

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getMacAddress() { return macAddress; }
    public void setMacAddress(String macAddress) { this.macAddress = macAddress; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public List<DeviceLicense> getDeviceLicenses() { return deviceLicenses; }
    public void setDeviceLicenses(List<DeviceLicense> deviceLicenses) { this.deviceLicenses = deviceLicenses; }

    public void addDeviceLicense(DeviceLicense dl) {
        deviceLicenses.add(dl);
        dl.setDevice(this);
    }
}