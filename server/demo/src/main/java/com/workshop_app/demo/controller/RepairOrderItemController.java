package com.workshop_app.demo.controller;

import com.workshop_app.demo.service.RepairOrderItemService;
import com.workshop_app.demo.service.dto.RepairOrderItemDTO;
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
@RequestMapping("/api/v1/repair-order-item")
@CrossOrigin
@Tag(name = "Repair Order Item", description = "Repair order item endpoints")
public class RepairOrderItemController {
    private final RepairOrderItemService repairOrderItemService;

    public RepairOrderItemController(RepairOrderItemService repairOrderItemService) {
        this.repairOrderItemService = repairOrderItemService;
    }

    @GetMapping
    @Operation(summary = "List repair order items", description = "Returns all repair order items.")
    public List<RepairOrderItemDTO> findAll() {
        return repairOrderItemService.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Find repair order item by id", description = "Returns a repair order item by id.")
    public RepairOrderItemDTO findById(@Parameter(description = "Repair order item id") @PathVariable Long id) {
        return repairOrderItemService.findById(id);
    }

    @GetMapping("/repair-order/{repairOrderId}")
    @Operation(summary = "Find items by repair order", description = "Returns repair order items for a repair order.")
    public List<RepairOrderItemDTO> findByRepairOrderId(
            @Parameter(description = "Repair order id") @PathVariable Long repairOrderId) {
        return repairOrderItemService.findByRepairOrderId(repairOrderId);
    }

    @GetMapping("/service/{serviceId}")
    @Operation(summary = "Find items by service", description = "Returns repair order items for a service.")
    public List<RepairOrderItemDTO> findByServiceId(@Parameter(description = "Service id") @PathVariable Long serviceId) {
        return repairOrderItemService.findByServiceId(serviceId);
    }

    @GetMapping("/search/service-name")
    @Operation(summary = "Find items by service name", description = "Returns repair order items matching a service name.")
    public List<RepairOrderItemDTO> findByServiceName(
            @Parameter(description = "Service name") @RequestParam String serviceName) {
        return repairOrderItemService.findByServiceName(serviceName);
    }

    @GetMapping("/exists/repair-order/{repairOrderId}")
    @Operation(summary = "Check repair order item existence by repair order", description = "Returns true when a repair order has items.")
    public boolean existsByRepairOrderId(@Parameter(description = "Repair order id") @PathVariable Long repairOrderId) {
        return repairOrderItemService.existsByRepairOrderId(repairOrderId);
    }

    @GetMapping("/exists/service/{serviceId}")
    @Operation(summary = "Check repair order item existence by service", description = "Returns true when a service is used by repair order items.")
    public boolean existsByServiceId(@Parameter(description = "Service id") @PathVariable Long serviceId) {
        return repairOrderItemService.existsByServiceId(serviceId);
    }

    @PostMapping
    @Operation(summary = "Create repair order item", description = "Creates a new repair order item.")
    public ResponseEntity<RepairOrderItemDTO> create(@RequestBody RepairOrderItemDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(repairOrderItemService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update repair order item", description = "Updates an existing repair order item.")
    public RepairOrderItemDTO update(
            @Parameter(description = "Repair order item id") @PathVariable Long id,
            @RequestBody RepairOrderItemDTO request) {
        return repairOrderItemService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete repair order item", description = "Deletes a repair order item.")
    public ResponseEntity<Void> deleteById(@Parameter(description = "Repair order item id") @PathVariable Long id) {
        repairOrderItemService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
