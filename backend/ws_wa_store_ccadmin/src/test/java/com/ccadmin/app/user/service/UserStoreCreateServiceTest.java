package com.ccadmin.app.user.service;
import com.ccadmin.app.user.model.entity.UserStoreEntity;
import com.ccadmin.app.user.repository.UserStoreRepository;
import com.ccadmin.app.store.shared.StoreShared;
import com.ccadmin.app.store.model.entity.StoreEntity;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class UserStoreCreateServiceTest {
    private UserStoreEntity store(String code, String main) {
        var store = new UserStoreEntity(); store.StoreCod = code; store.UserCod = "USER";
        store.IsMainStore = main; store.CreationUser = "CREATOR"; return store;
    }
    @Test void removesAssignmentsAndChangesMainStorePreservingAudit() {
        var repository = mock(UserStoreRepository.class); var stores = mock(StoreShared.class);
        var removed = store("A", "S"); var retained = store("B", "N");
        when(repository.findAllByUserCod("USER")).thenReturn(List.of(removed, retained));
        when(stores.findById("B")).thenReturn(new StoreEntity());
        new UserStoreCreateService(repository, stores).save("USER", List.of(store("B", "S")), "ADMIN");
        assertEquals("I", removed.Status); assertEquals("N", removed.IsMainStore);
        assertEquals("A", retained.Status); assertEquals("S", retained.IsMainStore);
        assertEquals("CREATOR", retained.CreationUser); assertEquals("ADMIN", retained.ModifyUser);
        verify(repository).saveAll(any());
    }
    @Test void rejectsMultipleMainStores() {
        var repository = mock(UserStoreRepository.class); var stores = mock(StoreShared.class);
        when(stores.findById(anyString())).thenReturn(new StoreEntity());
        assertThrows(IllegalArgumentException.class, () -> new UserStoreCreateService(repository, stores)
            .save("USER", List.of(store("A", "S"), store("B", "S")), "ADMIN"));
        verifyNoInteractions(repository);
    }
    @Test void removesAllStores() {
        var repository = mock(UserStoreRepository.class); var existing = store("A", "S");
        when(repository.findAllByUserCod("USER")).thenReturn(List.of(existing));
        new UserStoreCreateService(repository, mock(StoreShared.class)).save("USER", List.of(), "ADMIN");
        assertEquals("I", existing.Status); assertEquals("N", existing.IsMainStore);
    }
}
