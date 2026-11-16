package com.example.carshop.entity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;
@Entity
@Getter
@Setter
@NoArgsConstructor
public class Feature {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 80)
    private String name;
    // H2 computes this key for every insert/update, including concurrent SQL writes.
    @Column(name = "normalized_name", nullable = false, unique = true,
            insertable = false, updatable = false,
            columnDefinition = "varchar(240) generated always as (lower(trim(name)))")
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private String normalizedName;
    @Column(length = 500)
    private String description;
}
