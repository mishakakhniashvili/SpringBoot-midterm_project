package com.example.carshop.entity;
import jakarta.persistence.*;
@Entity
public class Car {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 100)
    private String model;
    @Column(nullable = false, unique = true, length = 17)
    private String vin;
    @Column(nullable = false)
    private Integer modelYear;
    @Column(nullable = false, precision = 12, scale = 2)
    private java.math.BigDecimal price;
    @Column(length = 1000)
    private String description;
    
    private String photoPath;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "manufacturer_id", nullable = false)
    private Manufacturer manufacturer;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "dealership_id", nullable = false)
    private Dealership dealership;
    @org.hibernate.annotations.BatchSize(size = 100)
    @ManyToMany
    @JoinTable(name = "car_features", joinColumns = @JoinColumn(name = "car_id"), inverseJoinColumns = @JoinColumn(name = "feature_id"))
    private java.util.Set<Feature> features = new java.util.HashSet<>();
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getVin() { return vin; }
    public void setVin(String vin) { this.vin = vin; }
    public Integer getModelYear() { return modelYear; }
    public void setModelYear(Integer modelYear) { this.modelYear = modelYear; }
    public java.math.BigDecimal getPrice() { return price; }
    public void setPrice(java.math.BigDecimal price) { this.price = price; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getPhotoPath() { return photoPath; }
    public void setPhotoPath(String photoPath) { this.photoPath = photoPath; }
    public Manufacturer getManufacturer() { return manufacturer; }
    public void setManufacturer(Manufacturer manufacturer) { this.manufacturer = manufacturer; }
    public Dealership getDealership() { return dealership; }
    public void setDealership(Dealership dealership) { this.dealership = dealership; }
    public java.util.Set<Feature> getFeatures() { return features; }
    public void setFeatures(java.util.Set<Feature> features) { this.features = features; }
}
