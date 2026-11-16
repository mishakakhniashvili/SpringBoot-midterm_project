package com.example.carshop.repository;
import com.example.carshop.entity.Car;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.Optional;
@Repository
public interface CarRepository extends JpaRepository<Car, Long> {
    Optional<Car> findByVinIgnoreCase(String vin);
    boolean existsByManufacturerId(Long id);
    boolean existsByDealershipId(Long id);
    boolean existsByFeaturesId(Long id);

    @Query("""
        select c from Car c where
        (:keyword = '' or lower(c.model) like lower(concat('%', :keyword, '%'))
          or lower(c.manufacturer.name) like lower(concat('%', :keyword, '%')))
        and (:manufacturerId is null or c.manufacturer.id = :manufacturerId)
        and (:dealershipId is null or c.dealership.id = :dealershipId)
        and (:featureId is null or exists (select f.id from c.features f where f.id = :featureId))
        and (:minPrice is null or c.price >= :minPrice)
        and (:maxPrice is null or c.price <= :maxPrice)
        """)
    Page<Car> search(@Param("keyword") String keyword, @Param("manufacturerId") Long manufacturerId,
        @Param("dealershipId") Long dealershipId, @Param("featureId") Long featureId,
        @Param("minPrice") BigDecimal minPrice, @Param("maxPrice") BigDecimal maxPrice, Pageable pageable);

    @Query("""
        select distinct c from Car c join fetch c.manufacturer join fetch c.dealership
        left join fetch c.features where c.id = :id
        """)
    Optional<Car> findDetailedById(@Param("id") Long id);
}
