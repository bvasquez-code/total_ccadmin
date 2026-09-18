package com.ccadmin.app.shared.service;

import com.ccadmin.app.shared.interfaceccadmin.CcAdminRepository;
import com.ccadmin.app.shared.model.dto.ResponsePageSearchT;
import com.ccadmin.app.shared.model.dto.SearchDto;

public class SearchTService<T> {

    private final CcAdminRepository<T, ?> ccAdminRepository;

    public SearchTService(CcAdminRepository<T, ?> ccAdminRepository)
    {
        this.ccAdminRepository = ccAdminRepository;
    }

    public ResponsePageSearchT <T>findAll(SearchDto search, int Limit)
    {
        search.setLimit(Limit);

        return executeFindAll(search);
    }

    public ResponsePageSearchT <T>findAll(SearchDto search)
    {
        search.setLimit(search.Limit);

        return executeFindAll(search);
    }

    public ResponsePageSearchT <T>findAllStore(SearchDto search, int Limit)
    {
        search.setLimit(Limit);

        return executeFindAllStore(search);
    }

    public ResponsePageSearchT <T>findAllStore(SearchDto search)
    {
        search.setLimit(search.Limit);

        return executeFindAllStore(search);
    }
    public ResponsePageSearchT<T> findAllForExport(SearchDto search, int maximumRows)
    {
        search.Page = 1;
        search.setLimit(maximumRows);
        int total = this.ccAdminRepository.countByQueryText(search);
        if (total > maximumRows) {
            throw new IllegalArgumentException(
                    "La exportación admite hasta " + maximumRows + " filas. Reduzca los filtros.");
        }
        return new ResponsePageSearchT<>(
                this.ccAdminRepository.findByQueryText(search), search.Page, search.Limit, total);
    }

    private ResponsePageSearchT <T>executeFindAll(SearchDto search)
    {
        return new ResponsePageSearchT<>(
                this.ccAdminRepository.findByQueryText(search),
                search.Page,
                search.Limit,
                this.ccAdminRepository.countByQueryText(search)
        );
    }

    private ResponsePageSearchT <T>executeFindAllStore(SearchDto search)
    {
        return new ResponsePageSearchT<>(
                this.ccAdminRepository.findByQueryTextStore(search.Query,search.Query,search.StoreCod,search.Init,search.Limit),
                search.Page,
                search.Limit,
                this.ccAdminRepository.countByQueryTextStore(search.Query,search.Query,search.StoreCod)
        );
    }
}
