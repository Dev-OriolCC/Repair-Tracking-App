package com.workshop_app.demo.service.dto;

import com.workshop_app.demo.data.entity.RepairOrderEntity;
import com.workshop_app.demo.data.entity.RepairOrderPaymentEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RepairOrderPaymentDTO {
    private Long id;
    private Long repairOrderId;
    private RepairOrderEntity.RepairOrderStatus repairOrderStatus;
    private String clientName;
    private BigDecimal repairOrderTotal;
    private BigDecimal repairOrderAmountPaid;
    private BigDecimal repairOrderPendingAmount;
    private BigDecimal amount;
    private RepairOrderPaymentEntity.PaymentMethod paymentMethod;
    private String note;
    private String createdAt;
}
