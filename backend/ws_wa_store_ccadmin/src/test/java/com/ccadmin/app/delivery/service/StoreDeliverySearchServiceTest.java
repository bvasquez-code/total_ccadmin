package com.ccadmin.app.delivery.service;

import com.ccadmin.app.delivery.model.dto.StoreDeliveryContextDto;
import com.ccadmin.app.delivery.model.dto.StoreLocationRequestDto;
import com.ccadmin.app.sale.repository.StoreVirtualConfigRepository;
import com.ccadmin.app.store.model.idto.IStoreVirtualCandidateDto;
import com.ccadmin.app.store.repository.StoreRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StoreDeliverySearchServiceTest {

    @Mock
    private StoreRepository storeRepository;
    @Mock
    private StoreVirtualConfigRepository storeVirtualConfigRepository;
    @InjectMocks
    private StoreDeliverySearchService storeDeliverySearchService;

    @Test
    void selectsNearestStoreAndCalculatesAvailableModalities() {
        IStoreVirtualCandidateDto nearest = candidate(
                "T001",
                "Tienda Centro",
                new BigDecimal("-6.7714"),
                new BigDecimal("-79.8409"),
                "S",
                new BigDecimal("10"),
                "S",
                new BigDecimal("100"),
                "S"
        );
        IStoreVirtualCandidateDto distant = candidate(
                "T002",
                "Tienda Norte",
                new BigDecimal("-6.6000"),
                new BigDecimal("-79.9000"),
                "S",
                new BigDecimal("10"),
                "S",
                new BigDecimal("100"),
                "S"
        );
        when(storeRepository.findAllActiveVirtualCandidates()).thenReturn(List.of(distant, nearest));

        StoreLocationRequestDto request = new StoreLocationRequestDto();
        request.Latitude = new BigDecimal("-6.7715");
        request.Longitude = new BigDecimal("-79.8410");
        request.Address = "Chiclayo";

        StoreDeliveryContextDto result = storeDeliverySearchService.resolveLocation(request);

        assertEquals("T001", result.Store.StoreCod);
        assertEquals("S", result.AllowsAutomaticDelivery);
        assertEquals("S", result.AllowsScheduledDelivery);
        assertEquals("S", result.AllowsStorePickup);
    }

    private IStoreVirtualCandidateDto candidate(
            String storeCod,
            String name,
            BigDecimal latitude,
            BigDecimal longitude,
            String automatic,
            BigDecimal automaticRadius,
            String scheduled,
            BigDecimal scheduledRadius,
            String pickup
    ) {
        IStoreVirtualCandidateDto candidate = mock(
                IStoreVirtualCandidateDto.class,
                withSettings().lenient()
        );
        when(candidate.getStoreCod()).thenReturn(storeCod);
        when(candidate.getName()).thenReturn(name);
        when(candidate.getLatitude()).thenReturn(latitude);
        when(candidate.getLongitude()).thenReturn(longitude);
        when(candidate.getAllowsAutomaticDelivery()).thenReturn(automatic);
        when(candidate.getAutomaticDeliveryRadiusKm()).thenReturn(automaticRadius);
        when(candidate.getAllowsScheduledDelivery()).thenReturn(scheduled);
        when(candidate.getScheduledDeliveryMaxRadiusKm()).thenReturn(scheduledRadius);
        when(candidate.getAllowsStorePickup()).thenReturn(pickup);
        return candidate;
    }
}
