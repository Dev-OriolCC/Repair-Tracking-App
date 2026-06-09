package com.workshop_app.demo.controller;

import com.workshop_app.demo.data.entity.ServiceEntity;
import com.workshop_app.demo.service.ServiceService;
import com.workshop_app.demo.service.dto.ServiceDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/service")
@CrossOrigin
@Tag(name = "Service", description = "Service catalog endpoints")
public class ServiceController {
    private final ServiceService serviceService;

    public ServiceController(ServiceService serviceService) {
        this.serviceService = serviceService;
    }

    @GetMapping
    @Operation(summary = "List services", description = "Returns all services.")
    public List<ServiceDTO> findAll() {
        return serviceService.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Find service by id", description = "Returns a service by id.")
    public ServiceDTO findById(@Parameter(description = "Service id") @PathVariable Long id) {
        return serviceService.findById(id);
    }

    @GetMapping("/search")
    @Operation(summary = "Find service by name", description = "Returns a service by name.")
    public ServiceDTO findByName(@Parameter(description = "Service name") @RequestParam String name) {
        return serviceService.findByName(name);
    }

    @GetMapping("/category")
    @Operation(summary = "Find services by category", description = "Returns services matching a category.")
    public List<ServiceDTO> findByCategory(
            @Parameter(description = "Service category") @RequestParam ServiceEntity.ServiceCategory category) {
        return serviceService.findByCategory(category);
    }

    @GetMapping("/active")
    @Operation(summary = "List active services", description = "Returns active services.")
    public List<ServiceDTO> findActive() {
        return serviceService.findActive();
    }

    @GetMapping("/exists")
    @Operation(summary = "Check service name existence", description = "Returns true when a service exists by name.")
    public boolean existsByName(@Parameter(description = "Service name") @RequestParam String name) {
        return serviceService.existsByName(name);
    }

    @PostMapping
    @Operation(summary = "Create service", description = "Creates a new service catalog entry.")
    public ResponseEntity<ServiceDTO> create(@RequestBody ServiceDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(serviceService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update service", description = "Updates an existing service catalog entry.")
    public ServiceDTO update(
            @Parameter(description = "Service id") @PathVariable Long id,
            @RequestBody ServiceDTO request) {
        return serviceService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete service", description = "Deletes a service when no repair order items depend on it.")
    public ResponseEntity<Void> deleteById(@Parameter(description = "Service id") @PathVariable Long id) {
        serviceService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
