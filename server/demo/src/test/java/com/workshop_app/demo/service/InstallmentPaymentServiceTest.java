package com.workshop_app.demo.service;

import com.workshop_app.demo.data.entity.ClientEntity;
import com.workshop_app.demo.data.entity.InstallmentEntity;
import com.workshop_app.demo.data.entity.InstallmentPaymentEntity;
import com.workshop_app.demo.data.repository.InstallmentPaymentRepository;
import com.workshop_app.demo.data.repository.InstallmentRepository;
import com.workshop_app.demo.service.dto.InstallmentPaymentDTO;
import com.workshop_app.demo.service.exception.InvalidRequestException;
import com.workshop_app.demo.service.exception.ResourceNotFoundException;
import com.workshop_app.demo.service.impl.InstallmentPaymentServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.workshop_app.demo.data.entity.InstallmentEntity.InstallmentStatus.*;
import static com.workshop_app.demo.data.entity.InstallmentPaymentEntity.PaymentMethod.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InstallmentPaymentServiceTest {

    @Mock
    InstallmentPaymentRepository installmentPaymentRepository;

    @Mock
    InstallmentRepository installmentRepository;

    @InjectMocks
    InstallmentPaymentServiceImpl installmentPaymentService;

    @Test
    void findAllMapsPaymentInstallmentAndClientFieldsAndSkipsDeletedInstallments() {
        InstallmentEntity active = installment(1L, "Roberto Garcia", ACTIVE, BigDecimal.valueOf(1000), BigDecimal.valueOf(250));
        InstallmentEntity deleted = installment(2L, "Deleted Client", DELETED, BigDecimal.valueOf(1000), BigDecimal.ZERO);
        when(installmentPaymentRepository.findAll()).thenReturn(List.of(
                payment(3L, active, BigDecimal.valueOf(250), TRANSFER, "March payment"),
                payment(4L, deleted, BigDecimal.valueOf(100), CASH, null)
        ));

        List<InstallmentPaymentDTO> payments = installmentPaymentService.findAll();

        assertEquals(1, payments.size());
        assertEquals(3L, payments.get(0).getId());
        assertEquals(1L, payments.get(0).getInstallmentId());
        assertEquals("Roberto Garcia", payments.get(0).getClientName());
        assertEquals("Shimano Stella", payments.get(0).getArticle());
        assertEquals(0, payments.get(0).getInstallmentPendingAmount().compareTo(BigDecimal.valueOf(750)));
        assertEquals(TRANSFER, payments.get(0).getPaymentMethod());
    }

    @Test
    void findByIdReturnsDtoAndRejectsMissingOrDeletedInstallment() {
        InstallmentEntity active = installment(1L, "Roberto Garcia", ACTIVE, BigDecimal.valueOf(1000), BigDecimal.ZERO);
        InstallmentEntity deleted = installment(2L, "Deleted Client", DELETED, BigDecimal.valueOf(1000), BigDecimal.ZERO);
        when(installmentPaymentRepository.findById(3L)).thenReturn(Optional.of(payment(3L, active, BigDecimal.TEN, CASH, null)));
        when(installmentPaymentRepository.findById(4L)).thenReturn(Optional.of(payment(4L, deleted, BigDecimal.TEN, CASH, null)));
        when(installmentPaymentRepository.findById(99L)).thenReturn(Optional.empty());

        assertEquals(3L, installmentPaymentService.findById(3L).getId());
        assertThrows(ResourceNotFoundException.class, () -> installmentPaymentService.findById(4L));
        assertThrows(ResourceNotFoundException.class, () -> installmentPaymentService.findById(99L));
    }

    @Test
    void lookupHelpersValidateAndNormalizeInputs() {
        InstallmentEntity installment = installment(1L, "Roberto Garcia", ACTIVE, BigDecimal.TEN, BigDecimal.ZERO);
        InstallmentPaymentEntity payment = payment(2L, installment, BigDecimal.TEN, CARD, null);
        when(installmentRepository.existsByIdAndStatusNot(1L, DELETED)).thenReturn(true);
        when(installmentPaymentRepository.findAllByInstallment_Id(1L)).thenReturn(List.of(payment));
        when(installmentPaymentRepository.findAllByPaymentMethod(CARD)).thenReturn(List.of(payment));

        assertEquals(1, installmentPaymentService.findByInstallmentId(1L).size());
        assertEquals(1, installmentPaymentService.findByPaymentMethod(" card ").size());
        assertThrows(InvalidRequestException.class, () -> installmentPaymentService.findByInstallmentId(0L));
        assertThrows(InvalidRequestException.class, () -> installmentPaymentService.findByPaymentMethod("wire"));
    }

    @Test
    void createSavesPaymentAndSyncsInstallmentAmounts() {
        InstallmentEntity installment = installment(1L, "Roberto Garcia", ACTIVE, BigDecimal.valueOf(1000), BigDecimal.valueOf(250));
        InstallmentPaymentEntity existingPayment = payment(2L, installment, BigDecimal.valueOf(250), CASH, null);
        when(installmentRepository.findById(1L)).thenReturn(Optional.of(installment));
        when(installmentPaymentRepository.findAllByInstallment_Id(1L)).thenReturn(List.of(existingPayment));
        when(installmentPaymentRepository.save(any(InstallmentPaymentEntity.class))).thenAnswer(invocation -> {
            InstallmentPaymentEntity saved = invocation.getArgument(0);
            saved.setId(3L);
            return saved;
        });

        InstallmentPaymentDTO created = installmentPaymentService.create(new InstallmentPaymentDTO(
                null, 1L, null, null, null, null, null, null, BigDecimal.valueOf(300), TRANSFER, " April ", null
        ));

        assertEquals(3L, created.getId());
        assertEquals("April", created.getNote());
        assertEquals(0, installment.getAmountPaid().compareTo(BigDecimal.valueOf(550)));
        assertEquals(0, installment.getPendingAmount().compareTo(BigDecimal.valueOf(450)));

        ArgumentCaptor<InstallmentPaymentEntity> captor = ArgumentCaptor.forClass(InstallmentPaymentEntity.class);
        verify(installmentPaymentRepository).save(captor.capture());
        assertSame(installment, captor.getValue().getInstallment());
        verify(installmentRepository).save(installment);
    }

    @Test
    void createRejectsMissingFieldsMissingInstallmentDeletedInstallmentAndOverpayment() {
        assertThrows(InvalidRequestException.class, () -> installmentPaymentService.create(new InstallmentPaymentDTO(
                null, null, null, null, null, null, null, null, BigDecimal.TEN, CASH, null, null
        )));
        assertThrows(InvalidRequestException.class, () -> installmentPaymentService.create(new InstallmentPaymentDTO(
                null, 1L, null, null, null, null, null, null, BigDecimal.ZERO, CASH, null, null
        )));
        assertThrows(InvalidRequestException.class, () -> installmentPaymentService.create(new InstallmentPaymentDTO(
                null, 1L, null, null, null, null, null, null, BigDecimal.TEN, null, null, null
        )));

        when(installmentRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> installmentPaymentService.create(validRequest()));

        when(installmentRepository.findById(1L)).thenReturn(Optional.of(installment(1L, "Deleted Client", DELETED, BigDecimal.valueOf(1000), BigDecimal.ZERO)));
        assertThrows(ResourceNotFoundException.class, () -> installmentPaymentService.create(validRequest()));

        InstallmentEntity installment = installment(1L, "Roberto Garcia", ACTIVE, BigDecimal.valueOf(100), BigDecimal.ZERO);
        when(installmentRepository.findById(1L)).thenReturn(Optional.of(installment));
        when(installmentPaymentRepository.findAllByInstallment_Id(1L)).thenReturn(List.of(payment(2L, installment, BigDecimal.valueOf(90), CASH, null)));
        assertThrows(InvalidRequestException.class, () -> installmentPaymentService.create(new InstallmentPaymentDTO(
                null, 1L, null, null, null, null, null, null, BigDecimal.valueOf(11), CASH, null, null
        )));

        verify(installmentPaymentRepository, never()).save(any(InstallmentPaymentEntity.class));
    }

    @Test
    void updateChangesPaymentAndMarksInstallmentCompletedWhenPendingReachesZero() {
        InstallmentEntity installment = installment(1L, "Roberto Garcia", ACTIVE, BigDecimal.valueOf(1000), BigDecimal.valueOf(250));
        InstallmentPaymentEntity existingPayment = payment(2L, installment, BigDecimal.valueOf(250), CASH, null);
        when(installmentPaymentRepository.findById(2L)).thenReturn(Optional.of(existingPayment));
        when(installmentRepository.findById(1L)).thenReturn(Optional.of(installment));
        when(installmentPaymentRepository.findAllByInstallment_Id(1L)).thenReturn(List.of(existingPayment));
        when(installmentPaymentRepository.save(existingPayment)).thenReturn(existingPayment);

        InstallmentPaymentDTO updated = installmentPaymentService.update(2L, new InstallmentPaymentDTO(
                null, 1L, null, null, null, null, null, null, BigDecimal.valueOf(1000), CARD, "Final", null
        ));

        assertEquals(CARD, updated.getPaymentMethod());
        assertEquals(0, installment.getAmountPaid().compareTo(BigDecimal.valueOf(1000)));
        assertEquals(0, installment.getPendingAmount().compareTo(BigDecimal.ZERO));
        assertEquals(COMPLETED, installment.getStatus());
        verify(installmentRepository).save(installment);
    }

    @Test
    void updateThrowsWhenPaymentMissing() {
        when(installmentPaymentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> installmentPaymentService.update(99L, validRequest()));
    }

    @Test
    void deleteByIdDeletesPaymentAndSyncsInstallmentAmounts() {
        InstallmentEntity installment = installment(1L, "Roberto Garcia", ACTIVE, BigDecimal.valueOf(1000), BigDecimal.valueOf(500));
        InstallmentPaymentEntity deletedPayment = payment(2L, installment, BigDecimal.valueOf(200), CASH, null);
        InstallmentPaymentEntity remainingPayment = payment(3L, installment, BigDecimal.valueOf(300), TRANSFER, null);
        when(installmentPaymentRepository.findById(2L)).thenReturn(Optional.of(deletedPayment));
        when(installmentPaymentRepository.findAllByInstallment_Id(1L)).thenReturn(List.of(deletedPayment, remainingPayment));

        installmentPaymentService.deleteById(2L);

        verify(installmentPaymentRepository).deleteById(2L);
        assertEquals(0, installment.getAmountPaid().compareTo(BigDecimal.valueOf(300)));
        assertEquals(0, installment.getPendingAmount().compareTo(BigDecimal.valueOf(700)));
        verify(installmentRepository).save(installment);
    }

    @Test
    void existsHelpersValidateAndDelegate() {
        when(installmentRepository.existsByIdAndStatusNot(1L, DELETED)).thenReturn(true);
        when(installmentPaymentRepository.existsByInstallment_Id(1L)).thenReturn(true);
        when(installmentPaymentRepository.findAllByPaymentMethod(CASH)).thenReturn(List.of(
                payment(2L, installment(1L, "Roberto Garcia", ACTIVE, BigDecimal.TEN, BigDecimal.ZERO), BigDecimal.TEN, CASH, null)
        ));

        assertTrue(installmentPaymentService.existsByInstallmentId(1L));
        assertTrue(installmentPaymentService.existsByPaymentMethod("cash"));
        assertThrows(InvalidRequestException.class, () -> installmentPaymentService.existsByInstallmentId(null));
        assertThrows(InvalidRequestException.class, () -> installmentPaymentService.existsByPaymentMethod(" "));
    }

    private InstallmentPaymentDTO validRequest() {
        return new InstallmentPaymentDTO(
                null, 1L, null, null, null, null, null, null, BigDecimal.TEN, CASH, null, null
        );
    }

    private InstallmentPaymentEntity payment(
            Long id,
            InstallmentEntity installment,
            BigDecimal amount,
            InstallmentPaymentEntity.PaymentMethod paymentMethod,
            String note) {
        InstallmentPaymentEntity payment = new InstallmentPaymentEntity();
        payment.setId(id);
        payment.setInstallment(installment);
        payment.setAmount(amount);
        payment.setPaymentMethod(paymentMethod);
        payment.setNote(note);
        payment.setCreatedAt(LocalDateTime.of(2026, 4, 15, 11, 0));
        return payment;
    }

    private InstallmentEntity installment(
            Long id,
            String clientName,
            InstallmentEntity.InstallmentStatus status,
            BigDecimal totalAmount,
            BigDecimal amountPaid) {
        InstallmentEntity installment = new InstallmentEntity();
        installment.setId(id);
        installment.setClient(client(clientName));
        installment.setArticle("Shimano Stella");
        installment.setTotalAmount(totalAmount);
        installment.setAmountPaid(amountPaid);
        installment.setPendingAmount(totalAmount.subtract(amountPaid));
        installment.setStatus(status);
        return installment;
    }

    private ClientEntity client(String name) {
        ClientEntity client = new ClientEntity();
        client.setName(name);
        return client;
    }
}
