package com.example.carshop.repository;
import com.example.carshop.entity.Dealership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Repository;
import java.util.Optional;
@Repository
public interface DealershipRepository extends JpaRepository<Dealership, Long> {
    Page<Dealership> findByNameContainingIgnoreCase(String name, Pageable pageable);
    Optional<Dealership> findByNameIgnoreCase(String name);
}
