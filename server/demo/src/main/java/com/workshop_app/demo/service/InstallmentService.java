package com.workshop_app.demo.service;

import com.workshop_app.demo.service.dto.InstallmentDTO;

import java.util.List;

public interface InstallmentService {
    List<InstallmentDTO> findAll();

    InstallmentDTO findById(Long id);

    List<InstallmentDTO> findByClientName(String clientName);

    List<InstallmentDTO> findByStatus(String status);

    List<InstallmentDTO> findByClientNumber(String clientNumber);

    List<InstallmentDTO> findByArticle(String article);

    List<InstallmentDTO> findByComment(String comment);

    InstallmentDTO create(InstallmentDTO request);

    InstallmentDTO update(Long id, InstallmentDTO request);

    void deleteById(Long id);

    boolean existsByClientName(String clientName);

    boolean existsByStatus(String status);

    boolean existsByClientNumber(String clientNumber);

    boolean existsByArticle(String article);

    boolean existsByComment(String comment);
}
