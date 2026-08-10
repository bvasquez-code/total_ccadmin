package com.ccadmin.app.store.service;

import com.ccadmin.app.sale.model.entity.StoreVirtualConfigEntity;
import com.ccadmin.app.sale.repository.StoreVirtualConfigRepository;
import com.ccadmin.app.store.model.entity.StoreEntity;
import com.ccadmin.app.store.model.dto.StoreVirtualConfigRegisterDto;
import com.ccadmin.app.store.repository.CompanyRepository;
import com.ccadmin.app.store.repository.StoreRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StoreServiceTest {

    @Mock
    private StoreRepository storeRepository;
    @Mock
    private CompanyRepository companyRepository;
    @Mock
    private StoreVirtualConfigRepository storeVirtualConfigRepository;
    @InjectMocks
    private StoreService storeService;

    @Test
    void savesEditableVirtualStoreConfiguration() {
        StoreEntity store = new StoreEntity();
        store.StoreCod = "T001";
        StoreVirtualConfigEntity requestedConfig = validConfig(store.StoreCod);
        StoreVirtualConfigRegisterDto register = validRegister(store, requestedConfig);
        when(storeRepository.findByStoreCod(store.StoreCod)).thenReturn(Optional.of(store));
        when(storeVirtualConfigRepository.findByStoreCod(store.StoreCod)).thenReturn(Optional.empty());
        when(storeVirtualConfigRepository.save(any(StoreVirtualConfigEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        StoreVirtualConfigEntity result = storeService.saveVirtualConfig(register);

        assertEquals(store.StoreCod, result.StoreCod);
        assertEquals(new BigDecimal("10"), result.AutomaticDeliveryRadiusKm);
        assertEquals(new BigDecimal("750"), result.ScheduledDeliveryMaxRadiusKm);
        assertEquals("SISTEMA", result.CreationUser);
        assertEquals("A", result.Status);
    }

    @Test
    void rejectsScheduledRadiusSmallerThanAutomaticRadius() {
        StoreEntity store = new StoreEntity();
        store.StoreCod = "T001";
        StoreVirtualConfigEntity requestedConfig = validConfig(store.StoreCod);
        requestedConfig.ScheduledDeliveryMaxRadiusKm = new BigDecimal("5");
        StoreVirtualConfigRegisterDto register = validRegister(store, requestedConfig);
        when(storeRepository.findByStoreCod(store.StoreCod)).thenReturn(Optional.of(store));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> storeService.saveVirtualConfig(register)
        );

        assertEquals("El alcance programado no puede ser menor al radio automatico", exception.getMessage());
        verify(storeVirtualConfigRepository, never()).save(any());
    }

    @Test
    void disablesVirtualStoreAndPreservesDeliveryConfiguration() {
        StoreEntity persistedStore = new StoreEntity();
        persistedStore.StoreCod = "T001";
        persistedStore.IsVirtualStoreEnabled = "S";
        StoreVirtualConfigEntity persistedConfig = validConfig(persistedStore.StoreCod);

        StoreEntity requestedStore = new StoreEntity();
        requestedStore.StoreCod = persistedStore.StoreCod;
        requestedStore.IsVirtualStoreEnabled = "N";
        StoreVirtualConfigRegisterDto register = new StoreVirtualConfigRegisterDto();
        register.Store = requestedStore;

        when(storeRepository.findByStoreCod(persistedStore.StoreCod)).thenReturn(Optional.of(persistedStore));
        when(storeVirtualConfigRepository.findByStoreCod(persistedStore.StoreCod))
                .thenReturn(Optional.of(persistedConfig));

        StoreVirtualConfigEntity result = storeService.saveVirtualConfig(register);

        assertEquals("N", persistedStore.IsVirtualStoreEnabled);
        assertEquals(persistedConfig, result);
        verify(storeRepository).save(persistedStore);
        verify(storeVirtualConfigRepository, never()).save(any());
    }

    private StoreVirtualConfigEntity validConfig(String storeCod) {
        StoreVirtualConfigEntity config = new StoreVirtualConfigEntity();
        config.StoreCod = storeCod;
        config.AllowsAutomaticDelivery = "S";
        config.AutomaticDeliveryRadiusKm = new BigDecimal("10");
        config.AllowsScheduledDelivery = "S";
        config.ScheduledDeliveryMaxRadiusKm = new BigDecimal("750");
        config.AllowsStorePickup = "S";
        config.PreparationTimeMinutes = 60;
        return config;
    }

    private StoreVirtualConfigRegisterDto validRegister(
            StoreEntity store,
            StoreVirtualConfigEntity config
    ) {
        store.IsVirtualStoreEnabled = "S";
        store.Address = "Av. Principal 123";
        store.UbigeoCod = "140101";
        store.Latitude = new BigDecimal("-6.77137");
        store.Longitude = new BigDecimal("-79.84088");

        StoreVirtualConfigRegisterDto register = new StoreVirtualConfigRegisterDto();
        register.Store = store;
        register.Config = config;
        return register;
    }
}
