package com.ccadmin.app.system.service;

import com.ccadmin.app.system.model.dto.CounterfoilRegisterDto;
import com.ccadmin.app.system.model.entity.CounterfoilEntity;
import com.ccadmin.app.system.model.entity.CounterfoilStoreEntity;
import com.ccadmin.app.system.repository.CounterfoilRepository;
import com.ccadmin.app.system.repository.CounterfoilStoreRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CounterfoilCreateServiceTest {

    @Mock
    private CounterfoilRepository counterfoilRepository;
    @Mock
    private CounterfoilStoreRepository counterfoilStoreRepository;
    @InjectMocks
    private CounterfoilCreateService counterfoilCreateService;

    @Test
    void allowsSameSeriesForDifferentDocumentType() {
        CounterfoilRegisterDto request = validRegister("07", "F001");
        when(counterfoilRepository.findByDocTypeSeries("07", "F001")).thenReturn(Optional.empty());
        when(counterfoilRepository.existsById("07F001")).thenReturn(false);
        when(counterfoilRepository.save(request.counterfoil)).thenReturn(request.counterfoil);
        when(counterfoilStoreRepository.save(request.counterfoilStore)).thenReturn(request.counterfoilStore);

        CounterfoilRegisterDto result = counterfoilCreateService.save(request);

        assertSame(request.counterfoil, result.counterfoil);
        assertSame(request.counterfoilStore, result.counterfoilStore);
        verify(counterfoilRepository).findByDocTypeSeries("07", "F001");
    }

    @Test
    void rejectsRepeatedSeriesForSameDocumentType() {
        CounterfoilRegisterDto request = validRegister("01", "F001");
        when(counterfoilRepository.findByDocTypeSeries("01", "F001"))
                .thenReturn(Optional.of(request.counterfoil));
        when(counterfoilRepository.existsById("01F001")).thenReturn(false);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> counterfoilCreateService.save(request)
        );

        assertEquals(
                "Ya existe un talonario para el tipo de documento 01 y la serie F001",
                exception.getMessage()
        );
        verify(counterfoilRepository, never()).save(request.counterfoil);
        verify(counterfoilStoreRepository, never()).save(request.counterfoilStore);
    }

    private CounterfoilRegisterDto validRegister(String documentType, String series) {
        CounterfoilEntity counterfoil = new CounterfoilEntity();
        counterfoil.CounterfoilCod = documentType + series;
        counterfoil.DocumentType = documentType;
        counterfoil.Series = series;
        counterfoil.Correlative = 0;
        counterfoil.IsAutomatic = "S";
        counterfoil.GroupDocument = "O";

        CounterfoilStoreEntity counterfoilStore = new CounterfoilStoreEntity();
        counterfoilStore.CounterfoilCod = counterfoil.CounterfoilCod;
        counterfoilStore.StoreCod = "T001";

        return new CounterfoilRegisterDto(counterfoil, counterfoilStore);
    }
}
