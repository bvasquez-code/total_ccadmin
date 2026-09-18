package com.ccadmin.app.shared.interfaceccadmin;

import com.ccadmin.app.shared.model.dto.SearchDto;

import java.util.List;

public interface CcAdminRepository<T, ID>{

    // Permite filtros especializados sin cambiar las búsquedas existentes.
    default int countByQueryText(SearchDto search)
    {
        return countByQueryText(search.Query, search.Query);
    }

    default List<T> findByQueryText(SearchDto search)
    {
        return findByQueryText(search.Query, search.Query, search.Init, search.Limit);
    }

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
