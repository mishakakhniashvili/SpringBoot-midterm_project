package com.example.carshop.service;
import lombok.RequiredArgsConstructor;
import com.example.carshop.dto.*;
import com.example.carshop.entity.Manufacturer;
import com.example.carshop.repository.*;
import com.example.carshop.exception.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.*;
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ManufacturerService {
    private final ManufacturerRepository repository;
    private final CarRepository cars;
    public Page<ReferenceResponse> list(String name, Pageable pageable) { return repository.findByNameContainingIgnoreCase(name, pageable).map(Responses::of); }
    public ReferenceResponse get(Long id) { return Responses.of(require(id)); }
    @Transactional
    public ReferenceResponse create(ReferenceRequest request) { return save(new Manufacturer(), request); }
    @Transactional
    public ReferenceResponse update(Long id, ReferenceRequest request) { return save(require(id), request); }
    @Transactional
    public void delete(Long id) {
        var entity = require(id);
        if (cars.existsByManufacturerId(id)) throw ApiException.conflict("Manufacturer is assigned to cars");
        repository.delete(entity);
    }
    private Manufacturer require(Long id) { return repository.findById(id).orElseThrow(() -> ApiException.notFound("Manufacturer")); }
    private ReferenceResponse save(Manufacturer entity, ReferenceRequest request) {
        repository.findByNameIgnoreCase(request.name().trim()).filter(e -> !e.getId().equals(entity.getId()))
            .ifPresent(e -> { throw ApiException.conflict("Manufacturer name already exists"); });
        entity.setName(request.name().trim()); entity.setDescription(request.description()); 
        return Responses.of(repository.saveAndFlush(entity));
    }
}
