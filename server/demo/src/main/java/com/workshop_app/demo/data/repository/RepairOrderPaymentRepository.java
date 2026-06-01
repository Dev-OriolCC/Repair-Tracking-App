package com.workshop_app.demo.data.repository;

import com.workshop_app.demo.data.entity.RepairOrderPaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RepairOrderPaymentRepository extends JpaRepository<RepairOrderPaymentEntity, Long> {

    List<RepairOrderPaymentEntity> findAllByRepairOrder_Id(Long repairOrderId);

    boolean existsByRepairOrder_Id(Long repairOrderId);

    List<RepairOrderPaymentEntity> findAllByPaymentMethod(RepairOrderPaymentEntity.PaymentMethod paymentMethod);

    boolean existsByPaymentMethod(RepairOrderPaymentEntity.PaymentMethod paymentMethod);
}
