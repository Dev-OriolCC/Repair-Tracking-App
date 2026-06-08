package com.workshop_app.demo.service;

import com.workshop_app.demo.service.dto.InstallmentPaymentDTO;

import java.util.List;

public interface InstallmentPaymentService {
    List<InstallmentPaymentDTO> findAll();

    InstallmentPaymentDTO findById(Long id);

    List<InstallmentPaymentDTO> findByInstallmentId(Long installmentId);

    List<InstallmentPaymentDTO> findByPaymentMethod(String paymentMethod);

    InstallmentPaymentDTO create(InstallmentPaymentDTO request);

    InstallmentPaymentDTO update(Long id, InstallmentPaymentDTO request);

    void deleteById(Long id);

    boolean existsByInstallmentId(Long installmentId);

    boolean existsByPaymentMethod(String paymentMethod);
}
