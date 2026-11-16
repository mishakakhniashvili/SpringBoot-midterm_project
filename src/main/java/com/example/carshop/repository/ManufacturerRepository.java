package com.example.carshop.repository;
import com.example.carshop.entity.Manufacturer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Repository;
import java.util.Optional;
@Repository
public interface ManufacturerRepository extends JpaRepository<Manufacturer, Long> {
    Page<Manufacturer> findByNameContainingIgnoreCase(String name, Pageable pageable);
    Optional<Manufacturer> findByNameIgnoreCase(String name);
}
