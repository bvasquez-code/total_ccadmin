package com.ccadmin.app.shared.service;

import com.ccadmin.app.shared.interfaceccadmin.CcAdminRepository;
import com.ccadmin.app.shared.model.dto.ResponsePageSearchT;
import com.ccadmin.app.shared.model.dto.SearchDto;

public class SearchTService<T> {

    private CcAdminRepository ccAdminRepository;

    public SearchTService(CcAdminRepository ccAdminRepository) {
        this.ccAdminRepository = ccAdminRepository;
    }

    public ResponsePageSearchT<T> findAll(SearchDto search, int Limit) {
        search.setLimit(Limit);
        return new ResponsePageSearchT(
                this.ccAdminRepository.findByQueryText(search.Query, search.Query, search.Init, search.Limit),
                search.Page,
                search.Limit,
                this.ccAdminRepository.countByQueryText(search.Query, search.Query)
        );
    }
}
