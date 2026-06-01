package com.workshop_app.demo.service;

import com.workshop_app.demo.data.entity.ClientEntity;
import com.workshop_app.demo.data.entity.RepairOrderEntity;
import com.workshop_app.demo.data.entity.RepairOrderPaymentEntity;
import com.workshop_app.demo.data.repository.RepairOrderItemRepository;
import com.workshop_app.demo.data.repository.RepairOrderPaymentRepository;
import com.workshop_app.demo.data.repository.RepairOrderRepository;
import com.workshop_app.demo.service.dto.RepairOrderPaymentDTO;
import com.workshop_app.demo.service.exception.InvalidRequestException;
import com.workshop_app.demo.service.exception.ResourceNotFoundException;
import com.workshop_app.demo.service.impl.RepairOrderPaymentServiceImpl;
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

import static com.workshop_app.demo.data.entity.RepairOrderEntity.RepairOrderStatus.COMPLETED;
import static com.workshop_app.demo.data.entity.RepairOrderEntity.RepairOrderStatus.IN_PROGRESS;
import static com.workshop_app.demo.data.entity.RepairOrderEntity.RepairOrderStatus.PENDING;
import static com.workshop_app.demo.data.entity.RepairOrderPaymentEntity.PaymentMethod.CARD;
import static com.workshop_app.demo.data.entity.RepairOrderPaymentEntity.PaymentMethod.CASH;
import static com.workshop_app.demo.data.entity.RepairOrderPaymentEntity.PaymentMethod.TRANSFER;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RepairOrderPaymentServiceTest {

    @Mock
    RepairOrderPaymentRepository repairOrderPaymentRepository;

    @Mock
    RepairOrderRepository repairOrderRepository;

    @Mock
    RepairOrderItemRepository repairOrderItemRepository;

    @InjectMocks
    RepairOrderPaymentServiceImpl repairOrderPaymentService;

    @Test
    void findAllMapsPaymentRepairOrderAndClientDisplayFields() {
        RepairOrderEntity repairOrder = repairOrder(1L, "Roberto Garcia", IN_PROGRESS, BigDecimal.valueOf(350), BigDecimal.valueOf(100));
        when(repairOrderPaymentRepository.findAll()).thenReturn(List.of(payment(
                2L, repairOrder, BigDecimal.valueOf(100), TRANSFER, "Down payment"
        )));

        List<RepairOrderPaymentDTO> payments = repairOrderPaymentService.findAll();

        assertEquals(1, payments.size());
        assertEquals(2L, payments.get(0).getId());
        assertEquals(1L, payments.get(0).getRepairOrderId());
        assertEquals(IN_PROGRESS, payments.get(0).getRepairOrderStatus());
        assertEquals("Roberto Garcia", payments.get(0).getClientName());
        assertEquals(0, payments.get(0).getRepairOrderTotal().compareTo(BigDecimal.valueOf(350)));
        assertEquals(0, payments.get(0).getRepairOrderAmountPaid().compareTo(BigDecimal.valueOf(100)));
        assertEquals(0, payments.get(0).getRepairOrderPendingAmount().compareTo(BigDecimal.valueOf(250)));
        assertEquals(TRANSFER, payments.get(0).getPaymentMethod());
    }

    @Test
    void findByIdReturnsDTO() {
        RepairOrderEntity repairOrder = repairOrder(1L, "Roberto Garcia", PENDING, BigDecimal.valueOf(350), BigDecimal.ZERO);
        when(repairOrderPaymentRepository.findById(2L)).thenReturn(Optional.of(payment(
                2L, repairOrder, BigDecimal.valueOf(50), CASH, "Cash payment"
        )));

        RepairOrderPaymentDTO payment = repairOrderPaymentService.findById(2L);

        assertEquals(2L, payment.getId());
        assertEquals(CASH, payment.getPaymentMethod());
        assertEquals("Cash payment", payment.getNote());
    }

    @Test
    void findByIdThrowsWhenMissing() {
        when(repairOrderPaymentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> repairOrderPaymentService.findById(99L));
    }

    @Test
    void lookupMethodsValidateAndNormalizeInputs() {
        RepairOrderEntity repairOrder = repairOrder(1L, "Roberto Garcia", PENDING, BigDecimal.TEN, BigDecimal.ZERO);
        RepairOrderPaymentEntity payment = payment(2L, repairOrder, BigDecimal.TEN, CARD, null);
        when(repairOrderPaymentRepository.findAllByRepairOrder_Id(1L)).thenReturn(List.of(payment));
        when(repairOrderPaymentRepository.findAllByPaymentMethod(CARD)).thenReturn(List.of(payment));

        assertEquals(1, repairOrderPaymentService.findByRepairOrderId(1L).size());
        assertEquals(1, repairOrderPaymentService.findByPaymentMethod(" card ").size());
        assertThrows(InvalidRequestException.class, () -> repairOrderPaymentService.findByRepairOrderId(0L));
        assertThrows(InvalidRequestException.class, () -> repairOrderPaymentService.findByPaymentMethod("wire"));

        verify(repairOrderPaymentRepository).findAllByRepairOrder_Id(1L);
        verify(repairOrderPaymentRepository).findAllByPaymentMethod(CARD);
    }

    @Test
    void createValidatesOrderItemsSavesPaymentAndSyncsRepairOrderAmounts() {
        RepairOrderEntity repairOrder = repairOrder(1L, "Roberto Garcia", IN_PROGRESS, BigDecimal.valueOf(350), BigDecimal.valueOf(50));
        RepairOrderPaymentEntity existingPayment = payment(2L, repairOrder, BigDecimal.valueOf(50), CASH, null);
        when(repairOrderRepository.findById(1L)).thenReturn(Optional.of(repairOrder));
        when(repairOrderItemRepository.existsByRepairOrder_Id(1L)).thenReturn(true);
        when(repairOrderPaymentRepository.findAllByRepairOrder_Id(1L)).thenReturn(List.of(existingPayment));
        when(repairOrderPaymentRepository.save(any(RepairOrderPaymentEntity.class))).thenAnswer(invocation -> {
            RepairOrderPaymentEntity savedPayment = invocation.getArgument(0);
            savedPayment.setId(3L);
            return savedPayment;
        });

        RepairOrderPaymentDTO createdPayment = repairOrderPaymentService.create(new RepairOrderPaymentDTO(
                null, 1L, null, null, null, null, null, BigDecimal.valueOf(125), TRANSFER, " Transfer ", null
        ));

        assertEquals(3L, createdPayment.getId());
        assertEquals(0, createdPayment.getAmount().compareTo(BigDecimal.valueOf(125)));
        assertEquals("Transfer", createdPayment.getNote());
        assertEquals(0, repairOrder.getAmountPaid().compareTo(BigDecimal.valueOf(175)));
        assertEquals(0, repairOrder.getPendingAmount().compareTo(BigDecimal.valueOf(175)));

        ArgumentCaptor<RepairOrderPaymentEntity> paymentCaptor = ArgumentCaptor.forClass(RepairOrderPaymentEntity.class);
        verify(repairOrderPaymentRepository).save(paymentCaptor.capture());
        assertSame(repairOrder, paymentCaptor.getValue().getRepairOrder());
        assertEquals(TRANSFER, paymentCaptor.getValue().getPaymentMethod());
        verify(repairOrderRepository).save(repairOrder);
    }

    @Test
    void createRejectsMissingFieldsMissingOrderOrderWithoutItemsAndOverpayment() {
        assertThrows(InvalidRequestException.class, () -> repairOrderPaymentService.create(new RepairOrderPaymentDTO(
                null, null, null, null, null, null, null, BigDecimal.TEN, CASH, null, null
        )));
        assertThrows(InvalidRequestException.class, () -> repairOrderPaymentService.create(new RepairOrderPaymentDTO(
                null, 1L, null, null, null, null, null, BigDecimal.ZERO, CASH, null, null
        )));
        assertThrows(InvalidRequestException.class, () -> repairOrderPaymentService.create(new RepairOrderPaymentDTO(
                null, 1L, null, null, null, null, null, BigDecimal.TEN, null, null, null
        )));

        when(repairOrderRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> repairOrderPaymentService.create(new RepairOrderPaymentDTO(
                null, 1L, null, null, null, null, null, BigDecimal.TEN, CASH, null, null
        )));

        RepairOrderEntity repairOrder = repairOrder(1L, "Roberto Garcia", PENDING, BigDecimal.valueOf(100), BigDecimal.ZERO);
        when(repairOrderRepository.findById(1L)).thenReturn(Optional.of(repairOrder));
        when(repairOrderItemRepository.existsByRepairOrder_Id(1L)).thenReturn(false);
        assertThrows(InvalidRequestException.class, () -> repairOrderPaymentService.create(new RepairOrderPaymentDTO(
                null, 1L, null, null, null, null, null, BigDecimal.TEN, CASH, null, null
        )));

        when(repairOrderItemRepository.existsByRepairOrder_Id(1L)).thenReturn(true);
        when(repairOrderPaymentRepository.findAllByRepairOrder_Id(1L)).thenReturn(List.of(
                payment(2L, repairOrder, BigDecimal.valueOf(90), CASH, null)
        ));
        assertThrows(InvalidRequestException.class, () -> repairOrderPaymentService.create(new RepairOrderPaymentDTO(
                null, 1L, null, null, null, null, null, BigDecimal.valueOf(11), CASH, null, null
        )));

        verify(repairOrderPaymentRepository, never()).save(any(RepairOrderPaymentEntity.class));
    }

    @Test
    void updateChangesPaymentAndMarksOrderCompletedWhenPendingReachesZero() {
        RepairOrderEntity repairOrder = repairOrder(1L, "Roberto Garcia", IN_PROGRESS, BigDecimal.valueOf(200), BigDecimal.valueOf(75));
        RepairOrderPaymentEntity existingPayment = payment(2L, repairOrder, BigDecimal.valueOf(75), CASH, null);
        when(repairOrderPaymentRepository.findById(2L)).thenReturn(Optional.of(existingPayment));
        when(repairOrderRepository.findById(1L)).thenReturn(Optional.of(repairOrder));
        when(repairOrderItemRepository.existsByRepairOrder_Id(1L)).thenReturn(true);
        when(repairOrderPaymentRepository.findAllByRepairOrder_Id(1L)).thenReturn(List.of(existingPayment));
        when(repairOrderPaymentRepository.save(existingPayment)).thenReturn(existingPayment);

        RepairOrderPaymentDTO updatedPayment = repairOrderPaymentService.update(2L, new RepairOrderPaymentDTO(
                null, 1L, null, null, null, null, null, BigDecimal.valueOf(200), CARD, "Paid in full", null
        ));

        assertEquals(CARD, updatedPayment.getPaymentMethod());
        assertEquals("Paid in full", updatedPayment.getNote());
        assertEquals(0, repairOrder.getAmountPaid().compareTo(BigDecimal.valueOf(200)));
        assertEquals(0, repairOrder.getPendingAmount().compareTo(BigDecimal.ZERO));
        assertEquals(COMPLETED, repairOrder.getStatus());
        verify(repairOrderRepository).save(repairOrder);
    }

    @Test
    void updateThrowsWhenPaymentIsMissing() {
        when(repairOrderPaymentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> repairOrderPaymentService.update(99L, new RepairOrderPaymentDTO(
                null, 1L, null, null, null, null, null, BigDecimal.TEN, CASH, null, null
        )));
    }

    @Test
    void deleteByIdDeletesPaymentAndSyncsRepairOrderAmounts() {
        RepairOrderEntity repairOrder = repairOrder(1L, "Roberto Garcia", IN_PROGRESS, BigDecimal.valueOf(200), BigDecimal.valueOf(150));
        RepairOrderPaymentEntity deletedPayment = payment(2L, repairOrder, BigDecimal.valueOf(50), CASH, null);
        RepairOrderPaymentEntity remainingPayment = payment(3L, repairOrder, BigDecimal.valueOf(100), TRANSFER, null);
        when(repairOrderPaymentRepository.findById(2L)).thenReturn(Optional.of(deletedPayment));
        when(repairOrderPaymentRepository.findAllByRepairOrder_Id(1L)).thenReturn(List.of(deletedPayment, remainingPayment));

        repairOrderPaymentService.deleteById(2L);

        verify(repairOrderPaymentRepository).deleteById(2L);
        assertEquals(0, repairOrder.getAmountPaid().compareTo(BigDecimal.valueOf(100)));
        assertEquals(0, repairOrder.getPendingAmount().compareTo(BigDecimal.valueOf(100)));
        verify(repairOrderRepository).save(repairOrder);
    }

    @Test
    void existsHelpersValidateBeforeChecking() {
        when(repairOrderPaymentRepository.existsByRepairOrder_Id(1L)).thenReturn(true);
        when(repairOrderPaymentRepository.existsByPaymentMethod(CASH)).thenReturn(true);

        assertTrue(repairOrderPaymentService.existsByRepairOrderId(1L));
        assertTrue(repairOrderPaymentService.existsByPaymentMethod("cash"));
        assertThrows(InvalidRequestException.class, () -> repairOrderPaymentService.existsByRepairOrderId(null));
        assertThrows(InvalidRequestException.class, () -> repairOrderPaymentService.existsByPaymentMethod(" "));

        verify(repairOrderPaymentRepository).existsByRepairOrder_Id(1L);
        verify(repairOrderPaymentRepository).existsByPaymentMethod(CASH);
    }

    private RepairOrderPaymentEntity payment(
            Long id,
            RepairOrderEntity repairOrder,
            BigDecimal amount,
            RepairOrderPaymentEntity.PaymentMethod paymentMethod,
            String note) {
        RepairOrderPaymentEntity payment = new RepairOrderPaymentEntity();
        payment.setId(id);
        payment.setRepairOrder(repairOrder);
        payment.setAmount(amount);
        payment.setPaymentMethod(paymentMethod);
        payment.setNote(note);
        payment.setCreatedAt(LocalDateTime.of(2026, 4, 6, 14, 0));
        return payment;
    }

    private RepairOrderEntity repairOrder(
            Long id,
            String clientName,
            RepairOrderEntity.RepairOrderStatus status,
            BigDecimal total,
            BigDecimal amountPaid) {
        RepairOrderEntity repairOrder = new RepairOrderEntity();
        repairOrder.setId(id);
        repairOrder.setClient(client(clientName));
        repairOrder.setStatus(status);
        repairOrder.setTotal(total);
        repairOrder.setAmountPaid(amountPaid);
        repairOrder.setPendingAmount(total.subtract(amountPaid));
        return repairOrder;
    }

    private ClientEntity client(String name) {
        ClientEntity client = new ClientEntity();
        client.setName(name);
        return client;
    }
}
