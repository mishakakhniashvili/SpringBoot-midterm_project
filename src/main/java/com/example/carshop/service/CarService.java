package com.example.carshop.service;
import lombok.RequiredArgsConstructor;
import com.example.carshop.dto.*;
import com.example.carshop.entity.*;
import com.example.carshop.repository.*;
import com.example.carshop.exception.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.*;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;
import java.math.BigDecimal;
import java.util.*;
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CarService {
    private final CarRepository cars;
    private final ManufacturerRepository manufacturers;
    private final DealershipRepository dealerships;
    private final FeatureRepository features;
    private final PhotoStorageService photos;
    public Page<CarResponse> list(String keyword, Long manufacturerId, Long dealershipId, Long featureId,
                                  BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable) {
        if ((minPrice != null && minPrice.signum() < 0) || (maxPrice != null && maxPrice.signum() < 0)
            || (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0))
            throw ApiException.badRequest("Invalid price range");
        return cars.search(keyword, manufacturerId, dealershipId, featureId, minPrice, maxPrice, pageable).map(Responses::of);
    }
    public CarResponse get(Long id) { return Responses.of(require(id)); }
    @Transactional
    public CarResponse create(CarRequest request, MultipartFile photo) {
        Car car = new Car(); apply(car, request);
        if (photo != null && !photo.isEmpty()) car.setPhotoPath(photos.store(photo));
        return Responses.of(cars.saveAndFlush(car));
    }
    @Transactional
    public CarResponse update(Long id, CarRequest request) {
        Car car = require(id); apply(car, request);
        return Responses.of(cars.saveAndFlush(car));
    }
    @Transactional
    public CarResponse updatePhoto(Long id, MultipartFile photo) {
        Car car = require(id);
        String old = car.getPhotoPath();
        car.setPhotoPath(photos.store(photo));
        cars.saveAndFlush(car); photos.deleteAfterCommit(old);
        return Responses.of(car);
    }
    public Resource photo(Long id) { return photos.load(require(id).getPhotoPath()); }
    @Transactional
    public void delete(Long id) {
        Car car = require(id); String old = car.getPhotoPath();
        cars.delete(car); cars.flush(); photos.deleteAfterCommit(old);
    }
    private Car require(Long id) { return cars.findDetailedById(id).orElseThrow(() -> ApiException.notFound("Car")); }
    private void apply(Car car, CarRequest request) {
        String vin = request.getVin().toUpperCase(Locale.ROOT);
        cars.findByVinIgnoreCase(vin).filter(c -> !c.getId().equals(car.getId()))
            .ifPresent(c -> { throw ApiException.conflict("VIN already exists"); });
        car.setManufacturer(manufacturers.findById(request.getManufacturerId()).orElseThrow(() -> ApiException.notFound("Manufacturer")));
        car.setDealership(dealerships.findById(request.getDealershipId()).orElseThrow(() -> ApiException.notFound("Dealership")));
        Set<Feature> selected = new HashSet<>(features.findAllById(request.getFeatureIds()));
        if (selected.size() != request.getFeatureIds().size()) throw ApiException.notFound("Feature");
        car.setFeatures(selected); car.setModel(request.getModel().trim()); car.setVin(vin);
        car.setModelYear(request.getModelYear()); car.setPrice(request.getPrice()); car.setDescription(request.getDescription());
    }
}
