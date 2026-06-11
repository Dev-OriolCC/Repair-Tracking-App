package com.workshop_app.demo.controller;

import com.workshop_app.demo.service.RepairOrderPaymentService;
import com.workshop_app.demo.service.dto.RepairOrderPaymentDTO;
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
@RequestMapping("/api/v1/repair-order-payment")
@CrossOrigin
@Tag(name = "Repair Order Payment", description = "Repair order payment endpoints")
public class RepairOrderPaymentController {
    private final RepairOrderPaymentService repairOrderPaymentService;

    public RepairOrderPaymentController(RepairOrderPaymentService repairOrderPaymentService) {
        this.repairOrderPaymentService = repairOrderPaymentService;
    }

    @GetMapping
    @Operation(summary = "List repair order payments", description = "Returns all repair order payments.")
    public List<RepairOrderPaymentDTO> findAll() {
        return repairOrderPaymentService.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Find repair order payment by id", description = "Returns a repair order payment by id.")
    public RepairOrderPaymentDTO findById(@Parameter(description = "Repair order payment id") @PathVariable Long id) {
        return repairOrderPaymentService.findById(id);
    }

    @GetMapping("/repair-order/{repairOrderId}")
    @Operation(summary = "Find payments by repair order", description = "Returns repair order payments for a repair order.")
    public List<RepairOrderPaymentDTO> findByRepairOrderId(
            @Parameter(description = "Repair order id") @PathVariable Long repairOrderId) {
        return repairOrderPaymentService.findByRepairOrderId(repairOrderId);
    }

    @GetMapping("/search/payment-method")
    @Operation(summary = "Find payments by payment method", description = "Returns repair order payments matching a payment method.")
    public List<RepairOrderPaymentDTO> findByPaymentMethod(
            @Parameter(description = "Payment method") @RequestParam String paymentMethod) {
        return repairOrderPaymentService.findByPaymentMethod(paymentMethod);
    }

    @GetMapping("/exists/repair-order/{repairOrderId}")
    @Operation(summary = "Check repair order payment existence by repair order", description = "Returns true when a repair order has payments.")
    public boolean existsByRepairOrderId(@Parameter(description = "Repair order id") @PathVariable Long repairOrderId) {
        return repairOrderPaymentService.existsByRepairOrderId(repairOrderId);
    }

    @GetMapping("/exists/payment-method")
    @Operation(summary = "Check repair order payment existence by payment method", description = "Returns true when a repair order payment exists with a payment method.")
    public boolean existsByPaymentMethod(@Parameter(description = "Payment method") @RequestParam String paymentMethod) {
        return repairOrderPaymentService.existsByPaymentMethod(paymentMethod);
    }

    @PostMapping
    @Operation(summary = "Create repair order payment", description = "Creates a new repair order payment.")
    public ResponseEntity<RepairOrderPaymentDTO> create(@RequestBody RepairOrderPaymentDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(repairOrderPaymentService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update repair order payment", description = "Updates an existing repair order payment.")
    public RepairOrderPaymentDTO update(
            @Parameter(description = "Repair order payment id") @PathVariable Long id,
            @RequestBody RepairOrderPaymentDTO request) {
        return repairOrderPaymentService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete repair order payment", description = "Deletes a repair order payment.")
    public ResponseEntity<Void> deleteById(@Parameter(description = "Repair order payment id") @PathVariable Long id) {
        repairOrderPaymentService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
