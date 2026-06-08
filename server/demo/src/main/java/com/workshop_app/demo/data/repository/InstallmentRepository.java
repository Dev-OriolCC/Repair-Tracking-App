package com.workshop_app.demo.data.repository;

import com.workshop_app.demo.data.entity.InstallmentEntity;
import com.workshop_app.demo.data.entity.InstallmentEntity.InstallmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface InstallmentRepository extends JpaRepository<InstallmentEntity, Long> {

    boolean existsByCreatedBy_Id(Long userId);

    boolean existsByClient_Id(Long clientId);

    List<InstallmentEntity> findAllByStatusNot(InstallmentStatus status);

    List<InstallmentEntity> findAllByClient_NameContainingIgnoreCaseAndStatusNot(String clientName, InstallmentStatus status);

    boolean existsByClient_NameContainingIgnoreCaseAndStatusNot(String clientName, InstallmentStatus status);

    List<InstallmentEntity> findAllByStatus(InstallmentStatus status);

    boolean existsByStatus(InstallmentStatus status);

    List<InstallmentEntity> findAllByClient_PhoneAndStatusNot(String phone, InstallmentStatus status);

    boolean existsByClient_PhoneAndStatusNot(String phone, InstallmentStatus status);

    List<InstallmentEntity> findAllByArticleContainingIgnoreCaseAndStatusNot(String article, InstallmentStatus status);

    boolean existsByArticleContainingIgnoreCaseAndStatusNot(String article, InstallmentStatus status);

    List<InstallmentEntity> findAllByCommentContainingIgnoreCaseAndStatusNot(String comment, InstallmentStatus status);

    boolean existsByCommentContainingIgnoreCaseAndStatusNot(String comment, InstallmentStatus status);

    boolean existsByIdAndStatusNot(Long id, InstallmentStatus status);

    List<InstallmentEntity> findAllByStatusIn(Collection<InstallmentStatus> statuses);
}
