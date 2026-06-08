package com.workshop_app.demo.data.repository;

import com.workshop_app.demo.data.entity.InstallmentPaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InstallmentPaymentRepository extends JpaRepository<InstallmentPaymentEntity, Long> {

    List<InstallmentPaymentEntity> findAllByInstallment_Id(Long installmentId);

    boolean existsByInstallment_Id(Long installmentId);

    List<InstallmentPaymentEntity> findAllByPaymentMethod(InstallmentPaymentEntity.PaymentMethod paymentMethod);

    boolean existsByPaymentMethod(InstallmentPaymentEntity.PaymentMethod paymentMethod);
}
