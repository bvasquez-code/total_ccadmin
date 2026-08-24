package com.ccadmin.app.system.service;

import com.ccadmin.app.system.model.entity.CounterfoilEntity;
import com.ccadmin.app.system.repository.CounterfoilRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CounterfoilSearchServiceTest {

    @Mock
    private CounterfoilRepository counterfoilRepository;
    @InjectMocks
    private CounterfoilSearchService counterfoilSearchService;

    @Test
    void checksExistenceUsingDocumentTypeAndSeries() {
        CounterfoilEntity counterfoil = new CounterfoilEntity();
        when(counterfoilRepository.findByDocTypeSeries("01", "F001"))
                .thenReturn(Optional.of(counterfoil));
        when(counterfoilRepository.findByDocTypeSeries("07", "F001"))
                .thenReturn(Optional.empty());

        assertTrue(counterfoilSearchService.existsByDocumentTypeAndSeries("01", "F001"));
        assertFalse(counterfoilSearchService.existsByDocumentTypeAndSeries("07", "F001"));
    }
}
