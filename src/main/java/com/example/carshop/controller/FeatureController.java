package com.example.carshop.controller;
import com.example.carshop.dto.*;
import com.example.carshop.service.FeatureService;
import jakarta.validation.Valid;
import org.springframework.data.domain.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import java.net.URI;
@RestController
@RequestMapping("/api/features")
public class FeatureController {
    private final FeatureService service;
    public FeatureController(FeatureService service) { this.service = service; }
    @GetMapping
    public Page<ReferenceResponse> list(@RequestParam(defaultValue="") String name, Pageable pageable) {
        return service.list(name, PageRequests.checked(pageable, "id", "name"));
    }
    @GetMapping("/{id}")
    public ReferenceResponse get(@PathVariable Long id) { return service.get(id); }
    @PostMapping
    public ResponseEntity<ReferenceResponse> create(@Valid @RequestBody ReferenceRequest request) {
        var result = service.create(request);
        return ResponseEntity.created(URI.create("/api/features/" + result.id())).body(result);
    }
    @PutMapping("/{id}")
    public ReferenceResponse update(@PathVariable Long id, @Valid @RequestBody ReferenceRequest request) { return service.update(id, request); }
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) { service.delete(id); }
}
