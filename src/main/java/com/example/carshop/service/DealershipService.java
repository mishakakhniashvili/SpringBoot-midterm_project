package com.example.carshop.service;
import lombok.RequiredArgsConstructor;
import com.example.carshop.dto.*;
import com.example.carshop.entity.Dealership;
import com.example.carshop.repository.*;
import com.example.carshop.exception.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.*;
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class DealershipService {
    private final DealershipRepository repository;
    private final CarRepository cars;
    public Page<ReferenceResponse> list(String name, Pageable pageable) { return repository.findByNameContainingIgnoreCase(name, pageable).map(Responses::of); }
    public ReferenceResponse get(Long id) { return Responses.of(require(id)); }
    @Transactional
    public ReferenceResponse create(DealershipRequest request) { return save(new Dealership(), request); }
    @Transactional
    public ReferenceResponse update(Long id, DealershipRequest request) { return save(require(id), request); }
    @Transactional
    public void delete(Long id) {
        var entity = require(id);
        if (cars.existsByDealershipId(id)) throw ApiException.conflict("Dealership is assigned to cars");
        repository.delete(entity);
    }
    private Dealership require(Long id) { return repository.findById(id).orElseThrow(() -> ApiException.notFound("Dealership")); }
    private ReferenceResponse save(Dealership entity, DealershipRequest request) {
        repository.findByNameIgnoreCase(request.name().trim()).filter(e -> !e.getId().equals(entity.getId()))
            .ifPresent(e -> { throw ApiException.conflict("Dealership name already exists"); });
        entity.setName(request.name().trim()); entity.setDescription(request.description()); entity.setEmail(request.email().trim());
        return Responses.of(repository.saveAndFlush(entity));
    }
}
