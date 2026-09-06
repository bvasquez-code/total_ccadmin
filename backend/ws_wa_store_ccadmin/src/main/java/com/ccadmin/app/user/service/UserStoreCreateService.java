package com.ccadmin.app.user.service;

import com.ccadmin.app.user.model.entity.UserStoreEntity;
import com.ccadmin.app.user.repository.UserStoreRepository;
import com.ccadmin.app.store.shared.StoreShared;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
public class UserStoreCreateService {
    private final UserStoreRepository userStoreRepository;
    private final StoreShared storeShared;

    public UserStoreCreateService(UserStoreRepository userStoreRepository, StoreShared storeShared) {
        this.userStoreRepository = userStoreRepository;
        this.storeShared = storeShared;
    }

    @Transactional
    public void save(String userCod, List<UserStoreEntity> requestedStores, String auditUser) {
        if (requestedStores == null) throw new IllegalArgumentException("Debe enviar las tiendas del usuario");
        Set<String> codes = new HashSet<>();
        for (UserStoreEntity store : requestedStores) {
            if (store.StoreCod == null || !codes.add(store.StoreCod)
                    || storeShared.findById(store.StoreCod) == null) {
                throw new IllegalArgumentException("La lista de tiendas no es valida");
            }
        }
        long mainCount = requestedStores.stream().filter(s -> "S".equals(s.IsMainStore)).count();
        if (!requestedStores.isEmpty() && mainCount != 1) {
            throw new IllegalArgumentException("Debe seleccionar exactamente una tienda principal");
        }
        Map<String, UserStoreEntity> assignments = new LinkedHashMap<>();
        for (UserStoreEntity existing : userStoreRepository.findAllByUserCod(userCod)) {
            existing.inactive(auditUser);
            existing.IsMainStore = "N";
            assignments.put(existing.StoreCod, existing);
        }
        for (UserStoreEntity requested : requestedStores) {
            UserStoreEntity assignment = assignments.get(requested.StoreCod);
            if (assignment == null) {
                assignment = new UserStoreEntity();
                assignment.UserCod = userCod;
                assignment.StoreCod = requested.StoreCod;
            }
            assignment.active(auditUser);
            assignment.IsMainStore = "S".equals(requested.IsMainStore) ? "S" : "N";
            assignments.put(assignment.StoreCod, assignment);
        }
        userStoreRepository.saveAll(assignments.values());
    }
}
