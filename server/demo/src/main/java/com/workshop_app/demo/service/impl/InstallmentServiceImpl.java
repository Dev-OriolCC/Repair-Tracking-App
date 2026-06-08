package com.workshop_app.demo.service.impl;

import com.workshop_app.demo.data.entity.ClientEntity;
import com.workshop_app.demo.data.entity.InstallmentEntity;
import com.workshop_app.demo.data.entity.InstallmentEntity.InstallmentStatus;
import com.workshop_app.demo.data.entity.UserEntity;
import com.workshop_app.demo.data.repository.ClientRepository;
import com.workshop_app.demo.data.repository.InstallmentRepository;
import com.workshop_app.demo.data.repository.UserRepository;
import com.workshop_app.demo.service.InstallmentService;
import com.workshop_app.demo.service.dto.InstallmentDTO;
import com.workshop_app.demo.service.exception.InvalidRequestException;
import com.workshop_app.demo.service.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
@Transactional
public class InstallmentServiceImpl implements InstallmentService {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH);
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+[0-9\\s\\-()]+$");

    private final InstallmentRepository installmentRepository;
    private final ClientRepository clientRepository;
    private final UserRepository userRepository;

    public InstallmentServiceImpl(
            InstallmentRepository installmentRepository,
            ClientRepository clientRepository,
            UserRepository userRepository) {
        this.installmentRepository = installmentRepository;
        this.clientRepository = clientRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<InstallmentDTO> findAll() {
        return installmentRepository.findAllByStatusNot(InstallmentStatus.DELETED)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public InstallmentDTO findById(Long id) {
        InstallmentEntity installment = installmentRepository.findById(validateId(id, "Installment id is required"))
                .orElseThrow(() -> new ResourceNotFoundException("Installment not found with id: " + id));
        if (installment.getStatus() == InstallmentStatus.DELETED) {
            throw new ResourceNotFoundException("Installment not found with id: " + id);
        }
        return toDTO(installment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InstallmentDTO> findByClientName(String clientName) {
        return installmentRepository.findAllByClient_NameContainingIgnoreCaseAndStatusNot(
                        normalizeRequiredText(clientName, "Client name is required"),
                        InstallmentStatus.DELETED)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<InstallmentDTO> findByStatus(String status) {
        return installmentRepository.findAllByStatus(normalizeStatus(status))
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<InstallmentDTO> findByClientNumber(String clientNumber) {
        return installmentRepository.findAllByClient_PhoneAndStatusNot(normalizePhone(clientNumber), InstallmentStatus.DELETED)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<InstallmentDTO> findByArticle(String article) {
        return installmentRepository.findAllByArticleContainingIgnoreCaseAndStatusNot(
                        normalizeRequiredText(article, "Installment article is required"),
                        InstallmentStatus.DELETED)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<InstallmentDTO> findByComment(String comment) {
        return installmentRepository.findAllByCommentContainingIgnoreCaseAndStatusNot(
                        normalizeRequiredText(comment, "Comment is required"),
                        InstallmentStatus.DELETED)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    public InstallmentDTO create(InstallmentDTO request) {
        ValidatedInstallmentRequest validatedRequest = validateRequest(request);
        ClientEntity client = findClient(validatedRequest.clientId());
        UserEntity createdBy = findUser(validatedRequest.createdById());

        InstallmentEntity installment = new InstallmentEntity();
        installment.setClient(client);
        installment.setCreatedBy(createdBy);
        applyRequest(installment, validatedRequest);
        return toDTO(installmentRepository.save(installment));
    }

    @Override
    public InstallmentDTO update(Long id, InstallmentDTO request) {
        ValidatedInstallmentRequest validatedRequest = validateRequest(request);
        InstallmentEntity installment = installmentRepository.findById(validateId(id, "Installment id is required"))
                .orElseThrow(() -> new ResourceNotFoundException("Installment not found with id: " + id));
        if (installment.getStatus() == InstallmentStatus.DELETED) {
            throw new ResourceNotFoundException("Installment not found with id: " + id);
        }
        ClientEntity client = findClient(validatedRequest.clientId());
        UserEntity createdBy = findUser(validatedRequest.createdById());

        installment.setClient(client);
        installment.setCreatedBy(createdBy);
        applyRequest(installment, validatedRequest);
        return toDTO(installmentRepository.save(installment));
    }

    @Override
    public void deleteById(Long id) {
        InstallmentEntity installment = installmentRepository.findById(validateId(id, "Installment id is required"))
                .orElseThrow(() -> new ResourceNotFoundException("Installment not found with id: " + id));
        installment.setStatus(InstallmentStatus.DELETED);
        installmentRepository.save(installment);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByClientName(String clientName) {
        return installmentRepository.existsByClient_NameContainingIgnoreCaseAndStatusNot(
                normalizeRequiredText(clientName, "Client name is required"),
                InstallmentStatus.DELETED);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByStatus(String status) {
        return installmentRepository.existsByStatus(normalizeStatus(status));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByClientNumber(String clientNumber) {
        return installmentRepository.existsByClient_PhoneAndStatusNot(normalizePhone(clientNumber), InstallmentStatus.DELETED);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByArticle(String article) {
        return installmentRepository.existsByArticleContainingIgnoreCaseAndStatusNot(
                normalizeRequiredText(article, "Installment article is required"),
                InstallmentStatus.DELETED);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByComment(String comment) {
        return installmentRepository.existsByCommentContainingIgnoreCaseAndStatusNot(
                normalizeRequiredText(comment, "Comment is required"),
                InstallmentStatus.DELETED);
    }

    private ValidatedInstallmentRequest validateRequest(InstallmentDTO request) {
        if (request == null) {
            throw new InvalidRequestException("Installment request is required");
        }
        Long clientId = validateId(request.getClientId(), "Installment client id is required");
        Long createdById = validateId(request.getCreatedById(), "Installment created by id is required");
        String article = normalizeRequiredText(request.getArticle(), "Installment article is required");
        BigDecimal interestRate = validateAmount(request.getInterestRate(), "Installment interest rate is required");
        BigDecimal totalAmount = validateAmount(request.getTotalAmount(), "Installment total amount is required");
        BigDecimal amountPaid = validateAmount(request.getAmountPaid(), "Installment amount paid is required");
        if (amountPaid.compareTo(totalAmount) > 0) {
            throw new InvalidRequestException("Installment amount paid cannot be greater than total amount");
        }

        return new ValidatedInstallmentRequest(
                clientId,
                createdById,
                article,
                normalizeOptionalText(request.getComment()),
                interestRate,
                totalAmount,
                amountPaid,
                totalAmount.subtract(amountPaid),
                request.getStatus() == null ? InstallmentStatus.ACTIVE : request.getStatus()
        );
    }

    private void applyRequest(InstallmentEntity installment, ValidatedInstallmentRequest request) {
        installment.setArticle(request.article());
        installment.setComment(request.comment());
        installment.setInterestRate(request.interestRate());
        installment.setTotalAmount(request.totalAmount());
        installment.setAmountPaid(request.amountPaid());
        installment.setPendingAmount(request.pendingAmount());
        installment.setStatus(request.status());
    }

    private ClientEntity findClient(Long clientId) {
        return clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found with id: " + clientId));
    }

    private UserEntity findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
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

    private BigDecimal validateAmount(BigDecimal amount, String requiredMessage) {
        if (amount == null) {
            throw new InvalidRequestException(requiredMessage);
        }
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidRequestException("Installment amounts cannot be negative");
        }
        return amount;
    }

    private InstallmentStatus normalizeStatus(String status) {
        String normalizedStatus = normalizeRequiredText(status, "Installment status is required")
                .toUpperCase(Locale.ROOT)
                .replace(" ", "_")
                .replace("-", "_");
        try {
            return InstallmentStatus.valueOf(normalizedStatus);
        } catch (IllegalArgumentException exception) {
            throw new InvalidRequestException("Installment status is invalid: " + status);
        }
    }

    private String normalizePhone(String phone) {
        String normalizedPhone = normalizeRequiredText(phone, "Client phone is required");
        if (!PHONE_PATTERN.matcher(normalizedPhone).matches()) {
            throw new InvalidRequestException("Client phone must be an international number starting with +");
        }

        String digits = normalizedPhone.replaceAll("\\D", "");
        if (digits.length() < 8 || digits.length() > 15) {
            throw new InvalidRequestException("Client phone must contain 8 to 15 digits");
        }
        return "+" + digits;
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

    private InstallmentDTO toDTO(InstallmentEntity installment) {
        ClientEntity client = installment.getClient();
        UserEntity createdBy = installment.getCreatedBy();
        return new InstallmentDTO(
                installment.getId(),
                client == null ? null : client.getId(),
                createdBy == null ? null : createdBy.getId(),
                client == null ? null : client.getName(),
                createdBy == null ? null : createdBy.getName(),
                installment.getArticle(),
                installment.getComment(),
                installment.getInterestRate(),
                installment.getTotalAmount(),
                installment.getAmountPaid(),
                installment.getPendingAmount(),
                installment.getStatus(),
                formatDate(installment.getCreatedAt()),
                formatDate(installment.getUpdatedAt())
        );
    }

    private String formatDate(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.format(DATE_FORMATTER);
    }

    private record ValidatedInstallmentRequest(
            Long clientId,
            Long createdById,
            String article,
            String comment,
            BigDecimal interestRate,
            BigDecimal totalAmount,
            BigDecimal amountPaid,
            BigDecimal pendingAmount,
            InstallmentStatus status) {
    }
}
