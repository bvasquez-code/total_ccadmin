package com.ccadmin.app.shared.interfaceccadmin;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CcAdminRepository<T, ID>{

    //method needs to be overridden
    default int countByQueryText(String id,String query)
    {
        return 0;
    }

    //method needs to be overridden
    default int countByQueryTextStore(String id,String query,String storeCod)
    {
        return 0;
    }

    //method needs to be overridden
    default List<T> findByQueryText(String id,String query,int init,int limit)
    {
        return null;
    }

    //method needs to be overridden
    default List<T> findByQueryTextStore(String id,String query,String storeCod,int init,int limit)
    {
        return null;
    }
}
