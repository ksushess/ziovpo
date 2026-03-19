package com.example.photoprintapplication1.models;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "product")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL)
    private List<License> licenses = new ArrayList<>();

    public Product() {}

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public List<License> getLicenses() { return licenses; }
    public void setLicenses(List<License> licenses) { this.licenses = licenses; }

    public void addLicense(License license) {
        licenses.add(license);
        license.setProduct(this);
    }
}