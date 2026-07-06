package com.ccadmin.app.sale.service;

import com.ccadmin.app.sale.model.entity.TaxEntity;
import com.ccadmin.app.sale.repository.TaxRepository;
import com.ccadmin.app.shared.model.dto.ResponsePageSearchT;
import com.ccadmin.app.shared.model.dto.ResponseWsDto;
import com.ccadmin.app.shared.model.dto.SearchDto;
import com.ccadmin.app.shared.service.SearchTService;
import com.ccadmin.app.system.utility.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TaxSearchService {

    @Autowired
    private TaxRepository taxRepository;

    private SearchTService<TaxEntity> searchTService;

    @Autowired
    private void initSearchService() {
        this.searchTService = new SearchTService<>(this.taxRepository);
    }

    public ResponsePageSearchT<TaxEntity> findAll(String query, int page) {
        return this.searchTService.findAll(new SearchDto(query, page), 10);
    }

    public TaxEntity findById(String taxCod) {
        return this.taxRepository.findById(taxCod).orElse(null);
    }

    public List<TaxEntity> findAllActive() {
        return this.taxRepository.findAllActive();
    }

    public ResponseWsDto findDataForm(String taxCod) {
        ResponseWsDto rpt = new ResponseWsDto();
        if (StringUtil.isNotEmpty(taxCod)) {
            rpt.AddResponseAdditional("tax", this.findById(taxCod));
        }
        return rpt;
    }
}
