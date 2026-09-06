package com.ccadmin.app.security.service;
import com.ccadmin.app.cash.repository.CashSessionRepository;
import com.ccadmin.app.security.model.entity.AppSessionEntity;
import com.ccadmin.app.security.repository.AppSessionRepository;
import com.ccadmin.app.user.model.entity.UserStoreEntity;
import com.ccadmin.app.user.shared.UserStoreShared;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class SessionStoreCreateServiceTest {
    @Test void selectsAssignedStoreAndItsCashSession() {
        var cash = mock(CashSessionRepository.class);
        var repository = mock(AppSessionRepository.class);
        var stores = mock(UserStoreShared.class);
        var service = new SessionStoreCreateService(cash) {
            public String getUserCod() { return "USER"; }
            public Long getSessionID() { return 1L; }
        };
        ReflectionTestUtils.setField(service, "userStoreShared", stores);
        ReflectionTestUtils.setField(service, "appSessionRepository", repository);
        var store = new UserStoreEntity(); store.StoreCod = "B";
        when(stores.findByUserCod("USER")).thenReturn(List.of(store));
        var session = new AppSessionEntity("USER", "TOKEN");
        session.SessionOjb = "{\"Other\":true}";
        when(repository.findActiveBySessionId(1L)).thenReturn(Optional.of(session));
        when(cash.findOpenIdByUserAndStore("USER", "B")).thenReturn(Optional.of(42L));
        service.selectStore("B");
        assertEquals("B", session.getSelectedStoreCod()); assertEquals(42L, session.CashSessionID);
        assertTrue(session.SessionOjb.contains("Other"));
        assertThrows(IllegalArgumentException.class, () -> service.selectStore("UNASSIGNED"));
        verify(repository, times(1)).save(session);
    }
}
