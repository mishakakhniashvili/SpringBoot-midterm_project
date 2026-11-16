package com.example.carshop.service;
import com.example.carshop.dto.*;
import com.example.carshop.entity.*;
import java.util.Comparator;
final class Responses {
    private Responses() {}
    static ReferenceResponse of(Manufacturer e) { return new ReferenceResponse(e.getId(), e.getName(), e.getDescription(), null); }
    static ReferenceResponse of(Feature e) { return new ReferenceResponse(e.getId(), e.getName(), e.getDescription(), null); }
    static ReferenceResponse of(Dealership e) { return new ReferenceResponse(e.getId(), e.getName(), e.getDescription(), e.getEmail()); }
    static CarResponse of(Car e) {
        return new CarResponse(e.getId(), e.getModel(), e.getVin(), e.getModelYear(), e.getPrice(), e.getDescription(),
            e.getPhotoPath(), of(e.getManufacturer()), of(e.getDealership()),
            e.getFeatures().stream().map(Responses::of).sorted(Comparator.comparing(ReferenceResponse::id)).toList());
    }
}
