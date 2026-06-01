package com.workshop_app.demo.service.impl;

import com.workshop_app.demo.data.entity.ClientEntity;
import com.workshop_app.demo.data.entity.RepairOrderEntity;
import com.workshop_app.demo.data.entity.RepairOrderPaymentEntity;
import com.workshop_app.demo.data.entity.RepairOrderPaymentEntity.PaymentMethod;
import com.workshop_app.demo.data.repository.RepairOrderItemRepository;
import com.workshop_app.demo.data.repository.RepairOrderPaymentRepository;
import com.workshop_app.demo.data.repository.RepairOrderRepository;
import com.workshop_app.demo.service.RepairOrderPaymentService;
import com.workshop_app.demo.service.dto.RepairOrderPaymentDTO;
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
public class RepairOrderPaymentServiceImpl implements RepairOrderPaymentService {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH);

    private final RepairOrderPaymentRepository repairOrderPaymentRepository;
    private final RepairOrderRepository repairOrderRepository;
    private final RepairOrderItemRepository repairOrderItemRepository;

    public RepairOrderPaymentServiceImpl(
            RepairOrderPaymentRepository repairOrderPaymentRepository,
            RepairOrderRepository repairOrderRepository,
            RepairOrderItemRepository repairOrderItemRepository) {
        this.repairOrderPaymentRepository = repairOrderPaymentRepository;
        this.repairOrderRepository = repairOrderRepository;
        this.repairOrderItemRepository = repairOrderItemRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RepairOrderPaymentDTO> findAll() {
        return repairOrderPaymentRepository.findAll()
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public RepairOrderPaymentDTO findById(Long id) {
        return repairOrderPaymentRepository.findById(id)
                .map(this::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Repair order payment not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RepairOrderPaymentDTO> findByRepairOrderId(Long repairOrderId) {
        return repairOrderPaymentRepository.findAllByRepairOrder_Id(validateId(repairOrderId, "Repair order id is required"))
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RepairOrderPaymentDTO> findByPaymentMethod(String paymentMethod) {
        return repairOrderPaymentRepository.findAllByPaymentMethod(normalizePaymentMethod(paymentMethod))
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    public RepairOrderPaymentDTO create(RepairOrderPaymentDTO request) {
        ValidatedRepairOrderPaymentRequest validatedRequest = validateRequest(request);
        RepairOrderEntity repairOrder = findRepairOrder(validatedRequest.repairOrderId());
        validateRepairOrderHasItems(repairOrder.getId());

        List<RepairOrderPaymentEntity> currentPayments = repairOrderPaymentRepository.findAllByRepairOrder_Id(repairOrder.getId());
        validateRepairOrderCanUseAmountPaid(repairOrder, sumPayments(currentPayments).add(validatedRequest.amount()));

        RepairOrderPaymentEntity payment = new RepairOrderPaymentEntity();
        payment.setRepairOrder(repairOrder);
        applyRequest(payment, validatedRequest);

        RepairOrderPaymentEntity savedPayment = repairOrderPaymentRepository.save(payment);
        syncRepairOrderPayments(repairOrder, withPayment(currentPayments, savedPayment));
        return toDTO(savedPayment);
    }

    @Override
    public RepairOrderPaymentDTO update(Long id, RepairOrderPaymentDTO request) {
        ValidatedRepairOrderPaymentRequest validatedRequest = validateRequest(request);
        RepairOrderPaymentEntity payment = repairOrderPaymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Repair order payment not found with id: " + id));
        RepairOrderEntity oldRepairOrder = payment.getRepairOrder();
        RepairOrderEntity repairOrder = findRepairOrder(validatedRequest.repairOrderId());
        validateRepairOrderHasItems(repairOrder.getId());

        List<RepairOrderPaymentEntity> newOrderPayments = repairOrderPaymentRepository.findAllByRepairOrder_Id(repairOrder.getId());
        validateRepairOrderCanUseAmountPaid(repairOrder, replacementTotal(newOrderPayments, payment.getId(), validatedRequest.amount(), true));

        boolean movedOrders = oldRepairOrder != null && !Objects.equals(oldRepairOrder.getId(), repairOrder.getId());
        List<RepairOrderPaymentEntity> oldOrderPayments = List.of();
        if (movedOrders) {
            oldOrderPayments = repairOrderPaymentRepository.findAllByRepairOrder_Id(oldRepairOrder.getId());
            validateRepairOrderCanUseAmountPaid(oldRepairOrder, totalWithoutPayment(oldOrderPayments, payment.getId()));
        }

        payment.setRepairOrder(repairOrder);
        applyRequest(payment, validatedRequest);

        RepairOrderPaymentEntity savedPayment = repairOrderPaymentRepository.save(payment);
        syncRepairOrderPayments(repairOrder, withReplacement(newOrderPayments, savedPayment));
        if (movedOrders) {
            syncRepairOrderPayments(oldRepairOrder, withoutPayment(oldOrderPayments, savedPayment.getId()));
        }
        return toDTO(savedPayment);
    }

    @Override
    public void deleteById(Long id) {
        RepairOrderPaymentEntity payment = repairOrderPaymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Repair order payment not found with id: " + id));
        RepairOrderEntity repairOrder = payment.getRepairOrder();
        List<RepairOrderPaymentEntity> currentPayments = repairOrderPaymentRepository.findAllByRepairOrder_Id(repairOrder.getId());
        List<RepairOrderPaymentEntity> remainingPayments = withoutPayment(currentPayments, payment.getId());

        repairOrderPaymentRepository.deleteById(payment.getId());
        syncRepairOrderPayments(repairOrder, remainingPayments);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByRepairOrderId(Long repairOrderId) {
        return repairOrderPaymentRepository.existsByRepairOrder_Id(validateId(repairOrderId, "Repair order id is required"));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByPaymentMethod(String paymentMethod) {
        return repairOrderPaymentRepository.existsByPaymentMethod(normalizePaymentMethod(paymentMethod));
    }

    private ValidatedRepairOrderPaymentRequest validateRequest(RepairOrderPaymentDTO request) {
        if (request == null) {
            throw new InvalidRequestException("Repair order payment request is required");
        }

        return new ValidatedRepairOrderPaymentRequest(
                validateId(request.getRepairOrderId(), "Repair order id is required"),
                validatePaymentAmount(request.getAmount()),
                validatePaymentMethod(request.getPaymentMethod()),
                normalizeOptionalText(request.getNote())
        );
    }

    private void applyRequest(RepairOrderPaymentEntity payment, ValidatedRepairOrderPaymentRequest request) {
        payment.setAmount(request.amount());
        payment.setPaymentMethod(request.paymentMethod());
        payment.setNote(request.note());
    }

    private RepairOrderEntity findRepairOrder(Long repairOrderId) {
        return repairOrderRepository.findById(repairOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("Repair order not found with id: " + repairOrderId));
    }

    private void validateRepairOrderHasItems(Long repairOrderId) {
        if (!repairOrderItemRepository.existsByRepairOrder_Id(repairOrderId)) {
            throw new InvalidRequestException("Repair order payment requires at least one repair order item");
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
            throw new InvalidRequestException("Repair order payment amount is required");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidRequestException("Repair order payment amount must be greater than zero");
        }
        return amount;
    }

    private PaymentMethod validatePaymentMethod(PaymentMethod paymentMethod) {
        if (paymentMethod == null) {
            throw new InvalidRequestException("Repair order payment method is required");
        }
        return paymentMethod;
    }

    private PaymentMethod normalizePaymentMethod(String paymentMethod) {
        String normalizedPaymentMethod = normalizeRequiredText(paymentMethod, "Repair order payment method is required")
                .toUpperCase(Locale.ROOT)
                .replace(" ", "_")
                .replace("-", "_");
        try {
            return PaymentMethod.valueOf(normalizedPaymentMethod);
        } catch (IllegalArgumentException exception) {
            throw new InvalidRequestException("Repair order payment method is invalid: " + paymentMethod);
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

    private void validateRepairOrderCanUseAmountPaid(RepairOrderEntity repairOrder, BigDecimal amountPaid) {
        BigDecimal total = repairOrder.getTotal() == null ? BigDecimal.ZERO : repairOrder.getTotal();
        if (amountPaid.compareTo(total) > 0) {
            throw new InvalidRequestException("Repair order amount paid cannot be greater than total");
        }
    }

    private void syncRepairOrderPayments(RepairOrderEntity repairOrder, List<RepairOrderPaymentEntity> payments) {
        BigDecimal amountPaid = sumPayments(payments);
        validateRepairOrderCanUseAmountPaid(repairOrder, amountPaid);

        BigDecimal total = repairOrder.getTotal() == null ? BigDecimal.ZERO : repairOrder.getTotal();
        repairOrder.setAmountPaid(amountPaid);
        repairOrder.setPendingAmount(total.subtract(amountPaid));
        if (repairOrder.getPendingAmount().compareTo(BigDecimal.ZERO) == 0) {
            repairOrder.setStatus(RepairOrderEntity.RepairOrderStatus.COMPLETED);
        }
        repairOrderRepository.save(repairOrder);
    }

    private BigDecimal sumPayments(List<RepairOrderPaymentEntity> payments) {
        return payments.stream()
                .map(RepairOrderPaymentEntity::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal replacementTotal(
            List<RepairOrderPaymentEntity> payments,
            Long paymentId,
            BigDecimal replacementAmount,
            boolean includeIfMissing) {
        boolean replaced = false;
        BigDecimal total = BigDecimal.ZERO;
        for (RepairOrderPaymentEntity payment : payments) {
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

    private BigDecimal totalWithoutPayment(List<RepairOrderPaymentEntity> payments, Long paymentId) {
        return sumPayments(withoutPayment(payments, paymentId));
    }

    private List<RepairOrderPaymentEntity> withPayment(
            List<RepairOrderPaymentEntity> payments,
            RepairOrderPaymentEntity addedPayment) {
        return java.util.stream.Stream.concat(payments.stream(), java.util.stream.Stream.of(addedPayment)).toList();
    }

    private List<RepairOrderPaymentEntity> withReplacement(
            List<RepairOrderPaymentEntity> payments,
            RepairOrderPaymentEntity replacementPayment) {
        boolean containsPayment = payments.stream().anyMatch(payment -> Objects.equals(payment.getId(), replacementPayment.getId()));
        List<RepairOrderPaymentEntity> replacedPayments = payments.stream()
                .map(payment -> Objects.equals(payment.getId(), replacementPayment.getId()) ? replacementPayment : payment)
                .toList();
        return containsPayment ? replacedPayments : withPayment(replacedPayments, replacementPayment);
    }

    private List<RepairOrderPaymentEntity> withoutPayment(List<RepairOrderPaymentEntity> payments, Long paymentId) {
        return payments.stream()
                .filter(payment -> !Objects.equals(payment.getId(), paymentId))
                .toList();
    }

    private RepairOrderPaymentDTO toDTO(RepairOrderPaymentEntity payment) {
        RepairOrderEntity repairOrder = payment.getRepairOrder();
        ClientEntity client = repairOrder == null ? null : repairOrder.getClient();
        return new RepairOrderPaymentDTO(
                payment.getId(),
                repairOrder == null ? null : repairOrder.getId(),
                repairOrder == null ? null : repairOrder.getStatus(),
                client == null ? null : client.getName(),
                repairOrder == null ? null : repairOrder.getTotal(),
                repairOrder == null ? null : repairOrder.getAmountPaid(),
                repairOrder == null ? null : repairOrder.getPendingAmount(),
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

    private record ValidatedRepairOrderPaymentRequest(
            Long repairOrderId,
            BigDecimal amount,
            PaymentMethod paymentMethod,
            String note) {
    }
}
