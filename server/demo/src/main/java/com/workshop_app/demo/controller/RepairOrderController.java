package com.workshop_app.demo.controller;

import com.workshop_app.demo.service.RepairOrderService;
import com.workshop_app.demo.service.dto.RepairOrderDTO;
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
@RequestMapping("/api/v1/repair-order")
@CrossOrigin
@Tag(name = "Repair Order", description = "Repair order endpoints")
public class RepairOrderController {
    private final RepairOrderService repairOrderService;

    public RepairOrderController(RepairOrderService repairOrderService) {
        this.repairOrderService = repairOrderService;
    }

    @GetMapping
    @Operation(summary = "List repair orders", description = "Returns all repair orders.")
    public List<RepairOrderDTO> findAll() {
        return repairOrderService.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Find repair order by id", description = "Returns a repair order by id.")
    public RepairOrderDTO findById(@Parameter(description = "Repair order id") @PathVariable Long id) {
        return repairOrderService.findById(id);
    }

    @GetMapping("/search/client-name")
    @Operation(summary = "Find repair orders by client name", description = "Returns repair orders matching a client name.")
    public List<RepairOrderDTO> findByClientName(
            @Parameter(description = "Client name") @RequestParam String clientName) {
        return repairOrderService.findByClientName(clientName);
    }

    @GetMapping("/search/status")
    @Operation(summary = "Find repair orders by status", description = "Returns repair orders matching a status.")
    public List<RepairOrderDTO> findByStatus(@Parameter(description = "Repair order status") @RequestParam String status) {
        return repairOrderService.findByStatus(status);
    }

    @GetMapping("/search/client-number")
    @Operation(summary = "Find repair orders by client number", description = "Returns repair orders matching a client phone number.")
    public List<RepairOrderDTO> findByClientNumber(
            @Parameter(description = "Client phone number") @RequestParam String clientNumber) {
        return repairOrderService.findByClientNumber(clientNumber);
    }

    @GetMapping("/search/comment")
    @Operation(summary = "Find repair orders by comment", description = "Returns repair orders matching comment text.")
    public List<RepairOrderDTO> findByComment(@Parameter(description = "Comment text") @RequestParam String comment) {
        return repairOrderService.findByComment(comment);
    }

    @GetMapping("/exists/client-name")
    @Operation(summary = "Check repair order client name existence", description = "Returns true when a repair order exists for a client name.")
    public boolean existsByClientName(@Parameter(description = "Client name") @RequestParam String clientName) {
        return repairOrderService.existsByClientName(clientName);
    }

    @GetMapping("/exists/status")
    @Operation(summary = "Check repair order status existence", description = "Returns true when a repair order exists with a status.")
    public boolean existsByStatus(@Parameter(description = "Repair order status") @RequestParam String status) {
        return repairOrderService.existsByStatus(status);
    }

    @GetMapping("/exists/client-number")
    @Operation(summary = "Check repair order client number existence", description = "Returns true when a repair order exists for a client phone number.")
    public boolean existsByClientNumber(@Parameter(description = "Client phone number") @RequestParam String clientNumber) {
        return repairOrderService.existsByClientNumber(clientNumber);
    }

    @GetMapping("/exists/comment")
    @Operation(summary = "Check repair order comment existence", description = "Returns true when a repair order exists with matching comment text.")
    public boolean existsByComment(@Parameter(description = "Comment text") @RequestParam String comment) {
        return repairOrderService.existsByComment(comment);
    }

    @PostMapping
    @Operation(summary = "Create repair order", description = "Creates a new repair order.")
    public ResponseEntity<RepairOrderDTO> create(@RequestBody RepairOrderDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(repairOrderService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update repair order", description = "Updates an existing repair order.")
    public RepairOrderDTO update(
            @Parameter(description = "Repair order id") @PathVariable Long id,
            @RequestBody RepairOrderDTO request) {
        return repairOrderService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete repair order", description = "Deletes a repair order.")
    public ResponseEntity<Void> deleteById(@Parameter(description = "Repair order id") @PathVariable Long id) {
        repairOrderService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
