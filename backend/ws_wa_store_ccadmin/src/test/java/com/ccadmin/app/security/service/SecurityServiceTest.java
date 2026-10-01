package com.ccadmin.app.security.service;

import com.ccadmin.app.cash.repository.CashSessionRepository;
import com.ccadmin.app.person.model.entity.PersonEntity;
import com.ccadmin.app.person.shared.PersonShared;
import com.ccadmin.app.security.model.dto.ApplicationInitializationStatusDto;
import com.ccadmin.app.security.model.dto.SessionStorageDto;
import com.ccadmin.app.security.model.entity.AppSessionEntity;
import com.ccadmin.app.security.model.entity.AppUserEntity;
import com.ccadmin.app.security.repository.AppSessionRepository;
import com.ccadmin.app.security.repository.AppUserRepository;
import com.ccadmin.app.user.shared.AppMenuShared;
import com.ccadmin.app.user.shared.UserStoreShared;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
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

    @Mock
    private AppUserRepository appUserRepository;
    @Mock
    private PersonShared personShared;
    @Mock
    private AppMenuShared appMenuShared;
    @Mock
    private ApplicationInitializationSearchService applicationInitializationSearchService;

    private SecurityService service;

    @BeforeEach
    void setUp() {
        service = spy(new SecurityService());
        ReflectionTestUtils.setField(service, "userStoreShared", userStoreShared);
        ReflectionTestUtils.setField(service, "cashSessionRepository", cashSessionRepository);
        ReflectionTestUtils.setField(service, "appSessionRepository", appSessionRepository);
        ReflectionTestUtils.setField(service, "appUserRepository", appUserRepository);
        ReflectionTestUtils.setField(service, "personShared", personShared);
        ReflectionTestUtils.setField(service, "appMenuShared", appMenuShared);
        ReflectionTestUtils.setField(service, "applicationInitializationSearchService", applicationInitializationSearchService);
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

    @ParameterizedTest
    @CsvSource({
            "' Ana ', ' Pérez ', Ana Pérez",
            "Ana, , Ana",
            ", Pérez, Pérez",
            ", , ''"
    })
    void loadsSessionNamesFromTheLinkedPerson(String names, String lastNames, String expectedName) {
        doReturn("USER01").when(service).getUserCod();
        doReturn(7L).when(service).getSessionID();
        AppSessionEntity appSession = new AppSessionEntity("USER01", "TOKEN", null);
        appSession.SessionID = 7L;
        AppUserEntity appUser = new AppUserEntity();
        appUser.UserCod = "USER01";
        appUser.PersonCod = "PERSON01";
        appUser.Email = "usuario@example.com";
        PersonEntity person = new PersonEntity();
        person.Names = names;
        person.LastNames = lastNames;
        person.Email = "persona@example.com";
        when(appSessionRepository.findActiveBySessionId(7L)).thenReturn(Optional.of(appSession));
        when(appUserRepository.findById("USER01")).thenReturn(Optional.of(appUser));
        when(personShared.findById("PERSON01")).thenReturn(person);
        when(userStoreShared.findByUserCod("USER01")).thenReturn(List.of());
        when(appMenuShared.findByUser("USER01")).thenReturn(List.of());
        when(applicationInitializationSearchService.findForUser("USER01"))
                .thenReturn(new ApplicationInitializationStatusDto());

        SessionStorageDto session = service.findUserSession();

        assertEquals(expectedName, session.Names);
        assertEquals("usuario@example.com", session.Email);
        assertEquals("PERSON01", session.PersonCod);
        verify(personShared).findById("PERSON01");
    }
}
