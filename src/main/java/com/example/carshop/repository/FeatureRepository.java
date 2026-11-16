package com.example.carshop.repository;
import com.example.carshop.entity.Feature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Repository;
import java.util.Optional;
@Repository
public interface FeatureRepository extends JpaRepository<Feature, Long> {
    Page<Feature> findByNameContainingIgnoreCase(String name, Pageable pageable);
    Optional<Feature> findByNameIgnoreCase(String name);
}
