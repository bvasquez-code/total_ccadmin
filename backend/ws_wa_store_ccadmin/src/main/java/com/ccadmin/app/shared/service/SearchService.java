package com.ccadmin.app.shared.service;

import com.ccadmin.app.shared.interfaceccadmin.CcAdminRepository;
import com.ccadmin.app.shared.model.dto.ResponsePageSearch;
import com.ccadmin.app.shared.model.dto.SearchDto;

import java.util.List;

public class SearchService {

    private CcAdminRepository ccAdminRepository;

    public SearchService(CcAdminRepository ccAdminRepository)
    {
        this.ccAdminRepository = ccAdminRepository;
    }

    public ResponsePageSearch findAll(SearchDto search, int Limit)
    {
        search.setLimit(Limit);

        return executeFindAll(search);
    }

    public ResponsePageSearch findAll(SearchDto search)
    {
        return executeFindAll(search);
    }

    public ResponsePageSearch findAllStore(SearchDto search, int Limit)
    {
        search.setLimit(Limit);

        return executeFindAllStore(search);
    }

    public ResponsePageSearch findAllStore(SearchDto search)
    {
        return executeFindAllStore(search);
    }
    private ResponsePageSearch executeFindAll(SearchDto search)
    {
        return new ResponsePageSearch(
                this.ccAdminRepository.findByQueryText(search.Query,search.Query,search.Init,search.Limit),
                search.Page,
                search.Limit,
                this.ccAdminRepository.countByQueryText(search.Query,search.Query)
        );
    }

    private ResponsePageSearch executeFindAllStore(SearchDto search)
    {
        return new ResponsePageSearch(
                this.ccAdminRepository.findByQueryTextStore(search.Query,search.Query,search.StoreCod,search.Init,search.Limit),
                search.Page,
                search.Limit,
                this.ccAdminRepository.countByQueryTextStore(search.Query,search.Query,search.StoreCod)
        );
    }
}
