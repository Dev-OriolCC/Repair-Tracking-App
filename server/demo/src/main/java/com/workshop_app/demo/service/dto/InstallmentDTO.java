package com.workshop_app.demo.service.dto;

import com.workshop_app.demo.data.entity.InstallmentEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class InstallmentDTO {
    private Long id;
    private Long clientId;
    private Long createdById;
    private String clientName;
    private String createdBy;
    private String article;
    private String comment;
    private BigDecimal interestRate;
    private BigDecimal totalAmount;
    private BigDecimal amountPaid;
    private BigDecimal pendingAmount;
    private InstallmentEntity.InstallmentStatus status;
    private String createdAt;
    private String updatedAt;
}
