package com.example.carshop.entity;
import jakarta.persistence.*;
@Entity
public class Feature {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 80)
    private String name;
    // H2 computes this key for every insert/update, including concurrent SQL writes.
    @Column(name = "normalized_name", nullable = false, unique = true,
            insertable = false, updatable = false,
            columnDefinition = "varchar(240) generated always as (lower(trim(name)))")
    private String normalizedName;
    @Column(length = 500)
    private String description;
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
