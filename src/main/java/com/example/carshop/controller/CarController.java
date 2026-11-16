package com.example.carshop.controller;
import com.example.carshop.dto.*;
import com.example.carshop.service.CarService;
import jakarta.validation.Valid;
import org.springframework.data.domain.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.*;
import org.springframework.core.io.Resource;
import java.net.URI;
import java.math.BigDecimal;
@RestController
@RequestMapping("/api/cars")
public class CarController {
    private final CarService service;
    public CarController(CarService service) { this.service = service; }
    @GetMapping
    public Page<CarResponse> list(@RequestParam(defaultValue="") String keyword,
        @RequestParam(required=false) Long manufacturerId, @RequestParam(required=false) Long dealershipId,
        @RequestParam(required=false) Long featureId, @RequestParam(required=false) BigDecimal minPrice,
        @RequestParam(required=false) BigDecimal maxPrice, Pageable pageable) {
        return service.list(keyword, manufacturerId, dealershipId, featureId, minPrice, maxPrice,
            PageRequests.checked(pageable, "id", "model", "modelYear", "price", "vin"));
    }
    @GetMapping("/{id}")
    public CarResponse get(@PathVariable Long id) { return service.get(id); }
    @PostMapping(consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CarResponse> create(@Valid @ModelAttribute CarRequest request,
        @RequestParam(required=false) MultipartFile photo) {
        var result = service.create(request, photo);
        return ResponseEntity.created(URI.create("/api/cars/" + result.id())).body(result);
    }
    @PutMapping("/{id}")
    public CarResponse update(@PathVariable Long id, @Valid @RequestBody CarRequest request) { return service.update(id, request); }
    @PutMapping(value="/{id}/photo", consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public CarResponse photo(@PathVariable Long id, @RequestParam MultipartFile photo) { return service.updatePhoto(id, photo); }
    @GetMapping("/{id}/photo")
    public ResponseEntity<Resource> photo(@PathVariable Long id) {
        Resource photo = service.photo(id);
        MediaType type = photo.getFilename().endsWith(".png") ? MediaType.IMAGE_PNG : MediaType.IMAGE_JPEG;
        return ResponseEntity.ok().contentType(type).header("X-Content-Type-Options", "nosniff").body(photo);
    }
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) { service.delete(id); }
}
