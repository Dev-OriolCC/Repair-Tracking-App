package com.workshop_app.demo.service;

import com.workshop_app.demo.data.entity.ClientEntity;
import com.workshop_app.demo.data.entity.InstallmentEntity;
import com.workshop_app.demo.data.entity.UserEntity;
import com.workshop_app.demo.data.repository.ClientRepository;
import com.workshop_app.demo.data.repository.InstallmentRepository;
import com.workshop_app.demo.data.repository.UserRepository;
import com.workshop_app.demo.service.dto.InstallmentDTO;
import com.workshop_app.demo.service.exception.InvalidRequestException;
import com.workshop_app.demo.service.exception.ResourceNotFoundException;
import com.workshop_app.demo.service.impl.InstallmentServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static com.workshop_app.demo.data.entity.InstallmentEntity.InstallmentStatus.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InstallmentServiceTest {

    @Mock
    InstallmentRepository installmentRepository;

    @Mock
    ClientRepository clientRepository;

    @Mock
    UserRepository userRepository;

    @InjectMocks
    InstallmentServiceImpl installmentService;

    @Test
    void findAllExcludesDeletedAndMapsDisplayFields() {
        InstallmentEntity installment = installment(1L, "Roberto Garcia", "Admin User", ACTIVE,
                BigDecimal.valueOf(1000), BigDecimal.valueOf(250));
        installment.setArticle("Shimano Stella");
        when(installmentRepository.findAllByStatusNot(DELETED)).thenReturn(List.of(installment));

        List<InstallmentDTO> installments = installmentService.findAll();

        assertEquals(1, installments.size());
        assertEquals(1L, installments.get(0).getId());
        assertEquals("Roberto Garcia", installments.get(0).getClientName());
        assertEquals("Admin User", installments.get(0).getCreatedBy());
        assertEquals("Shimano Stella", installments.get(0).getArticle());
        assertEquals(0, installments.get(0).getPendingAmount().compareTo(BigDecimal.valueOf(750)));
        verify(installmentRepository).findAllByStatusNot(DELETED);
    }

    @Test
    void findByIdReturnsDtoAndRejectsDeletedOrMissing() {
        when(installmentRepository.findById(1L)).thenReturn(Optional.of(installment(
                1L, "Roberto Garcia", "Admin User", ACTIVE, BigDecimal.valueOf(1000), BigDecimal.ZERO
        )));
        when(installmentRepository.findById(2L)).thenReturn(Optional.of(installment(
                2L, "Deleted Client", "Admin User", DELETED, BigDecimal.valueOf(1000), BigDecimal.ZERO
        )));
        when(installmentRepository.findById(99L)).thenReturn(Optional.empty());

        assertEquals(1L, installmentService.findById(1L).getId());
        assertThrows(ResourceNotFoundException.class, () -> installmentService.findById(2L));
        assertThrows(ResourceNotFoundException.class, () -> installmentService.findById(99L));
    }

    @Test
    void lookupHelpersNormalizeAndExcludeDeletedByDefault() {
        InstallmentEntity installment = installment(1L, "Roberto Garcia", "Admin User", ACTIVE, BigDecimal.TEN, BigDecimal.ZERO);
        when(installmentRepository.findAllByClient_NameContainingIgnoreCaseAndStatusNot("Roberto", DELETED)).thenReturn(List.of(installment));
        when(installmentRepository.findAllByStatus(DELETED)).thenReturn(List.of(installment));
        when(installmentRepository.findAllByClient_PhoneAndStatusNot("+529981112233", DELETED)).thenReturn(List.of(installment));
        when(installmentRepository.findAllByArticleContainingIgnoreCaseAndStatusNot("Shimano", DELETED)).thenReturn(List.of(installment));
        when(installmentRepository.findAllByCommentContainingIgnoreCaseAndStatusNot("Layaway", DELETED)).thenReturn(List.of(installment));

        assertEquals(1, installmentService.findByClientName(" Roberto ").size());
        assertEquals(1, installmentService.findByStatus("deleted").size());
        assertEquals(1, installmentService.findByClientNumber("+52 (998) 111-2233").size());
        assertEquals(1, installmentService.findByArticle(" Shimano ").size());
        assertEquals(1, installmentService.findByComment(" Layaway ").size());
        assertThrows(InvalidRequestException.class, () -> installmentService.findByStatus("closed"));
    }

    @Test
    void createResolvesLinksCalculatesPendingAndSaves() {
        ClientEntity client = client(1L, "Roberto Garcia", "+529981112233");
        UserEntity user = user(2L, "Admin User");
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(installmentRepository.save(any(InstallmentEntity.class))).thenAnswer(invocation -> {
            InstallmentEntity saved = invocation.getArgument(0);
            saved.setId(3L);
            return saved;
        });

        InstallmentDTO created = installmentService.create(new InstallmentDTO(
                null, 1L, 2L, null, null, " Stella ", " Layaway ", BigDecimal.valueOf(5),
                BigDecimal.valueOf(1000), BigDecimal.valueOf(250), null, null, null, null
        ));

        assertEquals(3L, created.getId());
        assertEquals("Stella", created.getArticle());
        assertEquals("Layaway", created.getComment());
        assertEquals(ACTIVE, created.getStatus());
        assertEquals(0, created.getPendingAmount().compareTo(BigDecimal.valueOf(750)));
        verify(installmentRepository).save(any(InstallmentEntity.class));
    }

    @Test
    void createRejectsInvalidRequestMissingLinksAndOverpayment() {
        assertThrows(InvalidRequestException.class, () -> installmentService.create(null));
        assertThrows(InvalidRequestException.class, () -> installmentService.create(new InstallmentDTO(
                null, null, 2L, null, null, "Stella", null, BigDecimal.ZERO,
                BigDecimal.TEN, BigDecimal.ZERO, null, ACTIVE, null, null
        )));
        assertThrows(InvalidRequestException.class, () -> installmentService.create(new InstallmentDTO(
                null, 1L, 2L, null, null, " ", null, BigDecimal.ZERO,
                BigDecimal.TEN, BigDecimal.ZERO, null, ACTIVE, null, null
        )));
        assertThrows(InvalidRequestException.class, () -> installmentService.create(new InstallmentDTO(
                null, 1L, 2L, null, null, "Stella", null, BigDecimal.ZERO,
                BigDecimal.TEN, BigDecimal.valueOf(11), null, ACTIVE, null, null
        )));

        when(clientRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> installmentService.create(validRequest()));

        when(clientRepository.findById(1L)).thenReturn(Optional.of(client(1L, "Roberto Garcia", "+529981112233")));
        when(userRepository.findById(2L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> installmentService.create(validRequest()));

        verify(installmentRepository, never()).save(any(InstallmentEntity.class));
    }

    @Test
    void updateChangesFieldsAndRejectsDeletedInstallments() {
        InstallmentEntity installment = installment(1L, "Roberto Garcia", "Admin User", ACTIVE, BigDecimal.valueOf(1000), BigDecimal.ZERO);
        when(installmentRepository.findById(1L)).thenReturn(Optional.of(installment));
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client(1L, "New Client", "+529981112233")));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L, "Admin User")));
        when(installmentRepository.save(installment)).thenReturn(installment);

        InstallmentDTO updated = installmentService.update(1L, validRequest());

        assertEquals("Stella", updated.getArticle());
        assertEquals(0, updated.getPendingAmount().compareTo(BigDecimal.valueOf(750)));

        when(installmentRepository.findById(3L)).thenReturn(Optional.of(installment(
                3L, "Deleted Client", "Admin User", DELETED, BigDecimal.valueOf(1000), BigDecimal.ZERO
        )));
        assertThrows(ResourceNotFoundException.class, () -> installmentService.update(3L, validRequest()));
    }

    @Test
    void deleteByIdSoftDeletesWithoutHardDelete() {
        InstallmentEntity installment = installment(1L, "Roberto Garcia", "Admin User", ACTIVE, BigDecimal.valueOf(1000), BigDecimal.ZERO);
        when(installmentRepository.findById(1L)).thenReturn(Optional.of(installment));

        installmentService.deleteById(1L);

        assertEquals(DELETED, installment.getStatus());
        verify(installmentRepository).save(installment);
        verify(installmentRepository, never()).deleteById(any());
    }

    @Test
    void existsHelpersValidateAndDelegate() {
        when(installmentRepository.existsByClient_NameContainingIgnoreCaseAndStatusNot("Roberto", DELETED)).thenReturn(true);
        when(installmentRepository.existsByStatus(ACTIVE)).thenReturn(true);
        when(installmentRepository.existsByClient_PhoneAndStatusNot("+529981112233", DELETED)).thenReturn(true);
        when(installmentRepository.existsByArticleContainingIgnoreCaseAndStatusNot("Stella", DELETED)).thenReturn(true);
        when(installmentRepository.existsByCommentContainingIgnoreCaseAndStatusNot("Layaway", DELETED)).thenReturn(true);

        assertTrue(installmentService.existsByClientName("Roberto"));
        assertTrue(installmentService.existsByStatus("active"));
        assertTrue(installmentService.existsByClientNumber("+52 998 111 2233"));
        assertTrue(installmentService.existsByArticle("Stella"));
        assertTrue(installmentService.existsByComment("Layaway"));
    }

    private InstallmentDTO validRequest() {
        return new InstallmentDTO(
                null, 1L, 2L, null, null, " Stella ", " Layaway ", BigDecimal.valueOf(5),
                BigDecimal.valueOf(1000), BigDecimal.valueOf(250), null, ACTIVE, null, null
        );
    }

    private InstallmentEntity installment(
            Long id,
            String clientName,
            String userName,
            InstallmentEntity.InstallmentStatus status,
            BigDecimal totalAmount,
            BigDecimal amountPaid) {
        InstallmentEntity installment = new InstallmentEntity();
        installment.setId(id);
        installment.setClient(client(id, clientName, "+529981112233"));
        installment.setCreatedBy(user(id, userName));
        installment.setArticle("Shimano Stella");
        installment.setInterestRate(BigDecimal.ZERO);
        installment.setTotalAmount(totalAmount);
        installment.setAmountPaid(amountPaid);
        installment.setPendingAmount(totalAmount.subtract(amountPaid));
        installment.setStatus(status);
        return installment;
    }

    private ClientEntity client(Long id, String name, String phone) {
        ClientEntity client = new ClientEntity();
        client.setId(id);
        client.setName(name);
        client.setPhone(phone);
        return client;
    }

    private UserEntity user(Long id, String name) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setName(name);
        return user;
    }
}
