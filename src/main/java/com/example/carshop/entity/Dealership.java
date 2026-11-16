package com.example.carshop.entity;
import jakarta.persistence.*;
@Entity
public class Dealership {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 80)
    private String name;
    @Column(length = 500)
    private String description;
    @Column(nullable = false, length = 120)
    private String email;
    @OneToMany(mappedBy = "dealership")
    private java.util.List<Car> cars = new java.util.ArrayList<>();
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public java.util.List<Car> getCars() { return cars; }
    public void setCars(java.util.List<Car> cars) { this.cars = cars; }
}
