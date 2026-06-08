package com.workshop_app.demo.service.dto;

import com.workshop_app.demo.data.entity.InstallmentEntity;
import com.workshop_app.demo.data.entity.InstallmentPaymentEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class InstallmentPaymentDTO {
    private Long id;
    private Long installmentId;
    private InstallmentEntity.InstallmentStatus installmentStatus;
    private String clientName;
    private String article;
    private BigDecimal installmentTotalAmount;
    private BigDecimal installmentAmountPaid;
    private BigDecimal installmentPendingAmount;
    private BigDecimal amount;
    private InstallmentPaymentEntity.PaymentMethod paymentMethod;
    private String note;
    private String createdAt;
}
