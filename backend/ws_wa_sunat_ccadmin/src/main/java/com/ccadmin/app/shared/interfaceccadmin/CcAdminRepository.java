package com.ccadmin.app.shared.interfaceccadmin;

import java.util.List;

public interface CcAdminRepository<T, ID> {

    default int countByQueryText(String id, String query) {
        return 0;
    }

    default List<T> findByQueryText(String id, String query, int init, int limit) {
        return null;
    }
}
