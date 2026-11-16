package com.example.carshop.entity;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;
@Entity
@Getter
@Setter
@NoArgsConstructor
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
}
