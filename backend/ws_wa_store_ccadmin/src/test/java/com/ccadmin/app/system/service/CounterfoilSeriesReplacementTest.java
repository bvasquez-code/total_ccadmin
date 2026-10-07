package com.ccadmin.app.system.service;

import com.ccadmin.app.system.model.dto.CounterfoilRegisterDto;
import com.ccadmin.app.system.model.entity.CounterfoilEntity;
import com.ccadmin.app.system.model.entity.CounterfoilStoreEntity;
import com.ccadmin.app.system.repository.CounterfoilRepository;
import com.ccadmin.app.system.repository.CounterfoilStoreRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CounterfoilSeriesReplacementTest {
    @Mock private CounterfoilRepository counterfoilRepository;
    @Mock private CounterfoilStoreRepository counterfoilStoreRepository;
    private CounterfoilCreateService service;

    @BeforeEach
    void setUp() {
        service = new CounterfoilCreateService();
        ReflectionTestUtils.setField(service, "counterfoilRepository", counterfoilRepository);
        ReflectionTestUtils.setField(service, "counterfoilStoreRepository", counterfoilStoreRepository);
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("ROOT", "fixture"));
    }

    @AfterEach
    void clearSession() {
        SecurityContextHolder.clearContext();
    }

    private CounterfoilRegisterDto request(String series) {
        CounterfoilEntity counterfoil = new CounterfoilEntity();
        counterfoil.CounterfoilCod = "01" + series;
        counterfoil.DocumentType = "01";
        counterfoil.Series = series;
        counterfoil.Correlative = 125;
        counterfoil.IsAutomatic = "S";
        counterfoil.GroupDocument = "F";
        return new CounterfoilRegisterDto(counterfoil, assignment(counterfoil.CounterfoilCod, "T001"));
    }

    private CounterfoilStoreEntity assignment(String counterfoilCod, String storeCod) {
        CounterfoilStoreEntity assignment = new CounterfoilStoreEntity();
        assignment.CounterfoilCod = counterfoilCod;
        assignment.StoreCod = storeCod;
        return assignment;
    }

    private CounterfoilEntity previousCounterfoil() {
        CounterfoilEntity previous = request("F001").counterfoil;
        previous.Correlative = 7;
        previous.CreationUser = "SEED";
        previous.CreationDate = new Date(0);
        when(counterfoilRepository.findByIdForUpdate("01F001")).thenReturn(Optional.of(previous));
        return previous;
    }

    private void successfulWrites() {
        when(counterfoilRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(counterfoilStoreRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void preservesTheExistingSaveContractWhenThereIsNoReplacement() {
        CounterfoilRegisterDto request = request("F001");
        when(counterfoilRepository.existsById("01F001")).thenReturn(true);
        when(counterfoilRepository.findByDocTypeSeries("01", "F001"))
                .thenReturn(Optional.of(request.counterfoil));
        successfulWrites();
        assertSame(request.counterfoil, service.save(request).counterfoil);
        assertEquals("A", request.counterfoil.Status);
        verify(counterfoilRepository, never()).findById(any());
    }

    @Test
    void registersTheNewSeriesAndDisablesThePreviousOneWithoutChangingItsIdentityOrCorrelative() {
        CounterfoilEntity previous = previousCounterfoil();
        when(counterfoilStoreRepository.findStoresByCounterfoil("01F001"))
                .thenReturn(List.of(assignment("01F001", "T001")));
        successfulWrites();
        CounterfoilRegisterDto request = request("F777");
        request.PreviousCounterfoilCod = "01F001";
        request.counterfoil.CreationUser = "SEED";
        request.counterfoil.CreationDate = new Date(0);
        CounterfoilRegisterDto saved = service.save(request);
        assertEquals("01F777", saved.counterfoil.CounterfoilCod);
        assertEquals(125, saved.counterfoil.Correlative);
        assertEquals("ROOT", saved.counterfoil.CreationUser);
        assertTrue(saved.counterfoil.CreationDate.getTime() > 0);
        assertEquals("T001", saved.counterfoilStore.StoreCod);
        assertEquals("01F777", saved.counterfoilStore.CounterfoilCod);
        assertEquals("01F001", previous.CounterfoilCod);
        assertEquals("F001", previous.Series);
        assertEquals(7, previous.Correlative);
        assertEquals("SEED", previous.CreationUser);
        assertEquals(new Date(0), previous.CreationDate);
        assertEquals("I", previous.Status);
        verify(counterfoilRepository, never()).delete(any());
    }

    @Test
    void rejectsReplacementWithAnAlreadyRegisteredSeriesBeforeWriting() {
        previousCounterfoil();
        when(counterfoilStoreRepository.findStoresByCounterfoil("01F001"))
                .thenReturn(List.of(assignment("01F001", "T001")));
        when(counterfoilRepository.existsById("01F777")).thenReturn(true);
        CounterfoilRegisterDto request = request("F777");
        request.PreviousCounterfoilCod = "01F001";
        assertThrows(IllegalArgumentException.class, () -> service.save(request));
        verify(counterfoilRepository, never()).save(any());
        verify(counterfoilStoreRepository, never()).save(any());
    }

    @Test
    void doesNotDisableACounterfoilSharedWithAnotherStore() {
        CounterfoilEntity previous = previousCounterfoil();
        when(counterfoilStoreRepository.findStoresByCounterfoil("01F001"))
                .thenReturn(List.of(assignment("01F001", "T001"), assignment("01F001", "T002")));
        CounterfoilRegisterDto request = request("F777");
        request.PreviousCounterfoilCod = "01F001";
        assertThrows(IllegalArgumentException.class, () -> service.save(request));
        assertEquals("A", previous.Status);
        verify(counterfoilRepository, never()).save(any());
    }

    @Test
    void rejectsChangingTheDocumentTypeOfThePreviousCounterfoil() {
        previousCounterfoil();
        CounterfoilRegisterDto request = request("F777");
        request.PreviousCounterfoilCod = "01F001";
        request.counterfoil.DocumentType = "03";
        assertThrows(IllegalArgumentException.class, () -> service.save(request));
        verify(counterfoilRepository, never()).save(any());
        verifyNoInteractions(counterfoilStoreRepository);
    }

    @Test
    void rejectsAStaleReplacementWhenThePreviousCounterfoilIsAlreadyInactive() {
        CounterfoilEntity previous = previousCounterfoil();
        previous.Status = "I";
        CounterfoilRegisterDto request = request("F777");
        request.PreviousCounterfoilCod = "01F001";
        assertThrows(IllegalArgumentException.class, () -> service.save(request));
        verify(counterfoilRepository, never()).save(any());
        verifyNoInteractions(counterfoilStoreRepository);
    }

    @Test
    void keepsThePreviousCounterfoilActiveIfTheNewStoreAssignmentCannotBeSaved() {
        CounterfoilEntity previous = previousCounterfoil();
        when(counterfoilStoreRepository.findStoresByCounterfoil("01F001"))
                .thenReturn(List.of(assignment("01F001", "T001")));
        when(counterfoilRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(counterfoilStoreRepository.save(any())).thenThrow(new IllegalStateException("fixture failure"));
        CounterfoilRegisterDto request = request("F777");
        request.PreviousCounterfoilCod = "01F001";
        assertThrows(IllegalStateException.class, () -> service.save(request));
        assertEquals("A", previous.Status);
        verify(counterfoilRepository, times(1)).save(any());
    }
}
