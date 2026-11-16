package com.example.carshop.dto;
import java.math.BigDecimal;
import java.util.List;
public record CarResponse(Long id, String model, String vin, Integer modelYear, BigDecimal price,
    String description, String photoPath, ReferenceResponse manufacturer,
    ReferenceResponse dealership, List<ReferenceResponse> features) {}
