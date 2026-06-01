package com.workshop_app.demo.service;

import com.workshop_app.demo.service.dto.RepairOrderPaymentDTO;

import java.util.List;

public interface RepairOrderPaymentService {
    List<RepairOrderPaymentDTO> findAll();

    RepairOrderPaymentDTO findById(Long id);

    List<RepairOrderPaymentDTO> findByRepairOrderId(Long repairOrderId);

    List<RepairOrderPaymentDTO> findByPaymentMethod(String paymentMethod);

    RepairOrderPaymentDTO create(RepairOrderPaymentDTO request);

    RepairOrderPaymentDTO update(Long id, RepairOrderPaymentDTO request);

    void deleteById(Long id);

    boolean existsByRepairOrderId(Long repairOrderId);

    boolean existsByPaymentMethod(String paymentMethod);
}
