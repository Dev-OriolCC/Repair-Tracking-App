package com.workshop_app.demo.service.impl;

import com.workshop_app.demo.data.entity.ClientEntity;
import com.workshop_app.demo.data.entity.InstallmentEntity;
import com.workshop_app.demo.data.entity.InstallmentEntity.InstallmentStatus;
import com.workshop_app.demo.data.entity.InstallmentPaymentEntity;
import com.workshop_app.demo.data.entity.InstallmentPaymentEntity.PaymentMethod;
import com.workshop_app.demo.data.repository.InstallmentPaymentRepository;
import com.workshop_app.demo.data.repository.InstallmentRepository;
import com.workshop_app.demo.service.InstallmentPaymentService;
import com.workshop_app.demo.service.dto.InstallmentPaymentDTO;
import com.workshop_app.demo.service.exception.InvalidRequestException;
import com.workshop_app.demo.service.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
@Transactional
public class InstallmentPaymentServiceImpl implements InstallmentPaymentService {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH);

    private final InstallmentPaymentRepository installmentPaymentRepository;
    private final InstallmentRepository installmentRepository;

    public InstallmentPaymentServiceImpl(
            InstallmentPaymentRepository installmentPaymentRepository,
            InstallmentRepository installmentRepository) {
        this.installmentPaymentRepository = installmentPaymentRepository;
        this.installmentRepository = installmentRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<InstallmentPaymentDTO> findAll() {
        return installmentPaymentRepository.findAll()
                .stream()
                .filter(payment -> payment.getInstallment() == null || payment.getInstallment().getStatus() != InstallmentStatus.DELETED)
                .map(this::toDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public InstallmentPaymentDTO findById(Long id) {
        InstallmentPaymentEntity payment = installmentPaymentRepository.findById(validateId(id, "Installment payment id is required"))
                .orElseThrow(() -> new ResourceNotFoundException("Installment payment not found with id: " + id));
        if (payment.getInstallment() != null && payment.getInstallment().getStatus() == InstallmentStatus.DELETED) {
            throw new ResourceNotFoundException("Installment payment not found with id: " + id);
        }
        return toDTO(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InstallmentPaymentDTO> findByInstallmentId(Long installmentId) {
        Long validatedInstallmentId = validateId(installmentId, "Installment id is required");
        validateInstallmentIsNotDeleted(validatedInstallmentId);
        return installmentPaymentRepository.findAllByInstallment_Id(validatedInstallmentId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<InstallmentPaymentDTO> findByPaymentMethod(String paymentMethod) {
        return installmentPaymentRepository.findAllByPaymentMethod(normalizePaymentMethod(paymentMethod))
                .stream()
                .filter(payment -> payment.getInstallment() == null || payment.getInstallment().getStatus() != InstallmentStatus.DELETED)
                .map(this::toDTO)
                .toList();
    }

    @Override
    public InstallmentPaymentDTO create(InstallmentPaymentDTO request) {
        ValidatedInstallmentPaymentRequest validatedRequest = validateRequest(request);
        InstallmentEntity installment = findInstallment(validatedRequest.installmentId());

        List<InstallmentPaymentEntity> currentPayments = installmentPaymentRepository.findAllByInstallment_Id(installment.getId());
        validateInstallmentCanUseAmountPaid(installment, sumPayments(currentPayments).add(validatedRequest.amount()));

        InstallmentPaymentEntity payment = new InstallmentPaymentEntity();
        payment.setInstallment(installment);
        applyRequest(payment, validatedRequest);

        InstallmentPaymentEntity savedPayment = installmentPaymentRepository.save(payment);
        syncInstallmentPayments(installment, withPayment(currentPayments, savedPayment));
        return toDTO(savedPayment);
    }

    @Override
    public InstallmentPaymentDTO update(Long id, InstallmentPaymentDTO request) {
        ValidatedInstallmentPaymentRequest validatedRequest = validateRequest(request);
        InstallmentPaymentEntity payment = installmentPaymentRepository.findById(validateId(id, "Installment payment id is required"))
                .orElseThrow(() -> new ResourceNotFoundException("Installment payment not found with id: " + id));
        InstallmentEntity oldInstallment = payment.getInstallment();
        InstallmentEntity installment = findInstallment(validatedRequest.installmentId());

        List<InstallmentPaymentEntity> newInstallmentPayments = installmentPaymentRepository.findAllByInstallment_Id(installment.getId());
        validateInstallmentCanUseAmountPaid(installment, replacementTotal(newInstallmentPayments, payment.getId(), validatedRequest.amount(), true));

        boolean movedInstallments = oldInstallment != null && !Objects.equals(oldInstallment.getId(), installment.getId());
        List<InstallmentPaymentEntity> oldInstallmentPayments = List.of();
        if (movedInstallments) {
            oldInstallmentPayments = installmentPaymentRepository.findAllByInstallment_Id(oldInstallment.getId());
            validateInstallmentCanUseAmountPaid(oldInstallment, totalWithoutPayment(oldInstallmentPayments, payment.getId()));
        }

        payment.setInstallment(installment);
        applyRequest(payment, validatedRequest);

        InstallmentPaymentEntity savedPayment = installmentPaymentRepository.save(payment);
        syncInstallmentPayments(installment, withReplacement(newInstallmentPayments, savedPayment));
        if (movedInstallments) {
            syncInstallmentPayments(oldInstallment, withoutPayment(oldInstallmentPayments, savedPayment.getId()));
        }
        return toDTO(savedPayment);
    }

    @Override
    public void deleteById(Long id) {
        InstallmentPaymentEntity payment = installmentPaymentRepository.findById(validateId(id, "Installment payment id is required"))
                .orElseThrow(() -> new ResourceNotFoundException("Installment payment not found with id: " + id));
        InstallmentEntity installment = payment.getInstallment();
        if (installment != null && installment.getStatus() == InstallmentStatus.DELETED) {
            throw new ResourceNotFoundException("Installment payment not found with id: " + id);
        }
        List<InstallmentPaymentEntity> currentPayments = installmentPaymentRepository.findAllByInstallment_Id(installment.getId());
        List<InstallmentPaymentEntity> remainingPayments = withoutPayment(currentPayments, payment.getId());

        installmentPaymentRepository.deleteById(payment.getId());
        syncInstallmentPayments(installment, remainingPayments);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByInstallmentId(Long installmentId) {
        Long validatedInstallmentId = validateId(installmentId, "Installment id is required");
        return installmentRepository.existsByIdAndStatusNot(validatedInstallmentId, InstallmentStatus.DELETED)
                && installmentPaymentRepository.existsByInstallment_Id(validatedInstallmentId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByPaymentMethod(String paymentMethod) {
        return installmentPaymentRepository.findAllByPaymentMethod(normalizePaymentMethod(paymentMethod))
                .stream()
                .anyMatch(payment -> payment.getInstallment() == null || payment.getInstallment().getStatus() != InstallmentStatus.DELETED);
    }

    private ValidatedInstallmentPaymentRequest validateRequest(InstallmentPaymentDTO request) {
        if (request == null) {
            throw new InvalidRequestException("Installment payment request is required");
        }

        return new ValidatedInstallmentPaymentRequest(
                validateId(request.getInstallmentId(), "Installment id is required"),
                validatePaymentAmount(request.getAmount()),
                validatePaymentMethod(request.getPaymentMethod()),
                normalizeOptionalText(request.getNote())
        );
    }

    private void applyRequest(InstallmentPaymentEntity payment, ValidatedInstallmentPaymentRequest request) {
        payment.setAmount(request.amount());
        payment.setPaymentMethod(request.paymentMethod());
        payment.setNote(request.note());
    }

    private InstallmentEntity findInstallment(Long installmentId) {
        InstallmentEntity installment = installmentRepository.findById(installmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Installment not found with id: " + installmentId));
        if (installment.getStatus() == InstallmentStatus.DELETED) {
            throw new ResourceNotFoundException("Installment not found with id: " + installmentId);
        }
        return installment;
    }

    private void validateInstallmentIsNotDeleted(Long installmentId) {
        if (!installmentRepository.existsByIdAndStatusNot(installmentId, InstallmentStatus.DELETED)) {
            throw new ResourceNotFoundException("Installment not found with id: " + installmentId);
        }
    }

    private Long validateId(Long id, String message) {
        if (id == null) {
            throw new InvalidRequestException(message);
        }
        if (id <= 0) {
            throw new InvalidRequestException("Id must be greater than zero");
        }
        return id;
    }

    private BigDecimal validatePaymentAmount(BigDecimal amount) {
        if (amount == null) {
            throw new InvalidRequestException("Installment payment amount is required");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidRequestException("Installment payment amount must be greater than zero");
        }
        return amount;
    }

    private PaymentMethod validatePaymentMethod(PaymentMethod paymentMethod) {
        if (paymentMethod == null) {
            throw new InvalidRequestException("Installment payment method is required");
        }
        return paymentMethod;
    }

    private PaymentMethod normalizePaymentMethod(String paymentMethod) {
        String normalizedPaymentMethod = normalizeRequiredText(paymentMethod, "Installment payment method is required")
                .toUpperCase(Locale.ROOT)
                .replace(" ", "_")
                .replace("-", "_");
        try {
            return PaymentMethod.valueOf(normalizedPaymentMethod);
        } catch (IllegalArgumentException exception) {
            throw new InvalidRequestException("Installment payment method is invalid: " + paymentMethod);
        }
    }

    private String normalizeRequiredText(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new InvalidRequestException(message);
        }
        return value.trim();
    }

    private String normalizeOptionalText(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private void validateInstallmentCanUseAmountPaid(InstallmentEntity installment, BigDecimal amountPaid) {
        BigDecimal totalAmount = installment.getTotalAmount() == null ? BigDecimal.ZERO : installment.getTotalAmount();
        if (amountPaid.compareTo(totalAmount) > 0) {
            throw new InvalidRequestException("Installment amount paid cannot be greater than total amount");
        }
    }

    private void syncInstallmentPayments(InstallmentEntity installment, List<InstallmentPaymentEntity> payments) {
        BigDecimal amountPaid = sumPayments(payments);
        validateInstallmentCanUseAmountPaid(installment, amountPaid);

        BigDecimal totalAmount = installment.getTotalAmount() == null ? BigDecimal.ZERO : installment.getTotalAmount();
        installment.setAmountPaid(amountPaid);
        installment.setPendingAmount(totalAmount.subtract(amountPaid));
        if (installment.getPendingAmount().compareTo(BigDecimal.ZERO) == 0) {
            installment.setStatus(InstallmentStatus.COMPLETED);
        }
        installmentRepository.save(installment);
    }

    private BigDecimal sumPayments(List<InstallmentPaymentEntity> payments) {
        return payments.stream()
                .map(InstallmentPaymentEntity::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal replacementTotal(
            List<InstallmentPaymentEntity> payments,
            Long paymentId,
            BigDecimal replacementAmount,
            boolean includeIfMissing) {
        boolean replaced = false;
        BigDecimal total = BigDecimal.ZERO;
        for (InstallmentPaymentEntity payment : payments) {
            if (Objects.equals(payment.getId(), paymentId)) {
                total = total.add(replacementAmount);
                replaced = true;
            } else if (payment.getAmount() != null) {
                total = total.add(payment.getAmount());
            }
        }
        if (!replaced && includeIfMissing) {
            total = total.add(replacementAmount);
        }
        return total;
    }

    private BigDecimal totalWithoutPayment(List<InstallmentPaymentEntity> payments, Long paymentId) {
        return sumPayments(withoutPayment(payments, paymentId));
    }

    private List<InstallmentPaymentEntity> withPayment(
            List<InstallmentPaymentEntity> payments,
            InstallmentPaymentEntity addedPayment) {
        return java.util.stream.Stream.concat(payments.stream(), java.util.stream.Stream.of(addedPayment)).toList();
    }

    private List<InstallmentPaymentEntity> withReplacement(
            List<InstallmentPaymentEntity> payments,
            InstallmentPaymentEntity replacementPayment) {
        boolean containsPayment = payments.stream().anyMatch(payment -> Objects.equals(payment.getId(), replacementPayment.getId()));
        List<InstallmentPaymentEntity> replacedPayments = payments.stream()
                .map(payment -> Objects.equals(payment.getId(), replacementPayment.getId()) ? replacementPayment : payment)
                .toList();
        return containsPayment ? replacedPayments : withPayment(replacedPayments, replacementPayment);
    }

    private List<InstallmentPaymentEntity> withoutPayment(List<InstallmentPaymentEntity> payments, Long paymentId) {
        return payments.stream()
                .filter(payment -> !Objects.equals(payment.getId(), paymentId))
                .toList();
    }

    private InstallmentPaymentDTO toDTO(InstallmentPaymentEntity payment) {
        InstallmentEntity installment = payment.getInstallment();
        ClientEntity client = installment == null ? null : installment.getClient();
        return new InstallmentPaymentDTO(
                payment.getId(),
                installment == null ? null : installment.getId(),
                installment == null ? null : installment.getStatus(),
                client == null ? null : client.getName(),
                installment == null ? null : installment.getArticle(),
                installment == null ? null : installment.getTotalAmount(),
                installment == null ? null : installment.getAmountPaid(),
                installment == null ? null : installment.getPendingAmount(),
                payment.getAmount(),
                payment.getPaymentMethod(),
                payment.getNote(),
                formatDate(payment.getCreatedAt())
        );
    }

    private String formatDate(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.format(DATE_FORMATTER);
    }

    private record ValidatedInstallmentPaymentRequest(
            Long installmentId,
            BigDecimal amount,
            PaymentMethod paymentMethod,
            String note) {
    }
}
