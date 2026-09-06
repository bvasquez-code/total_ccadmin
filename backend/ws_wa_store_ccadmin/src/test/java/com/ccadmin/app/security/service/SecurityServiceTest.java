package com.ccadmin.app.security.service;

import com.ccadmin.app.cash.repository.CashSessionRepository;
import com.ccadmin.app.security.model.entity.AppSessionEntity;
import com.ccadmin.app.security.repository.AppSessionRepository;
import com.ccadmin.app.user.shared.UserStoreShared;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecurityServiceTest {

    @Mock
    private UserStoreShared userStoreShared;
    @Mock
    private CashSessionRepository cashSessionRepository;
    @Mock
    private AppSessionRepository appSessionRepository;

    private SecurityService service;

    @BeforeEach
    void setUp() {
        service = new SecurityService();
        ReflectionTestUtils.setField(service, "userStoreShared", userStoreShared);
        ReflectionTestUtils.setField(service, "cashSessionRepository", cashSessionRepository);
        ReflectionTestUtils.setField(service, "appSessionRepository", appSessionRepository);
    }

    @Test
    void carriesTheOpenCashSessionIntoTheNewApplicationSession() {
        when(userStoreShared.findByUserCod("USER01")).thenReturn(java.util.List.of(new com.ccadmin.app.user.model.entity.UserStoreEntity()));
        when(userStoreShared.getMainStore("USER01")).thenReturn("T001");
        when(cashSessionRepository.findOpenIdByUserAndStore("USER01", "T001"))
                .thenReturn(Optional.of(15L));

        service.createUserSession("USER01", "TOKEN");

        ArgumentCaptor<AppSessionEntity> sessionCaptor = ArgumentCaptor.forClass(AppSessionEntity.class);
        verify(appSessionRepository).save(sessionCaptor.capture());
        assertEquals("USER01", sessionCaptor.getValue().UserCod);
        assertEquals("TOKEN", sessionCaptor.getValue().Token);
        assertEquals("T001", sessionCaptor.getValue().getSelectedStoreCod());
        assertEquals(15L, sessionCaptor.getValue().CashSessionID);
    }
    @Test
    void multipleStoresRequireSelectionBeforeSettingCashContext() {
        when(userStoreShared.getMainStore("USER01")).thenReturn("T001");
        when(userStoreShared.findByUserCod("USER01")).thenReturn(java.util.List.of(
            new com.ccadmin.app.user.model.entity.UserStoreEntity(),
            new com.ccadmin.app.user.model.entity.UserStoreEntity()));
        when(cashSessionRepository.findOpenIdByUserAndStore("USER01", "T001"))
            .thenReturn(Optional.of(15L));
        service.createUserSession("USER01", "TOKEN");
        ArgumentCaptor<AppSessionEntity> captor = ArgumentCaptor.forClass(AppSessionEntity.class);
        verify(appSessionRepository).save(captor.capture());
        org.junit.jupiter.api.Assertions.assertNull(captor.getValue().getSelectedStoreCod());
        org.junit.jupiter.api.Assertions.assertNull(captor.getValue().CashSessionID);
    }
}
