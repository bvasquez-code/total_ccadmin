package com.ccadmin.app.sale.service;

import com.ccadmin.app.sale.model.entity.TaxAffectationEntity;
import com.ccadmin.app.sale.repository.TaxAffectationRepository;
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
public class TaxAffectationSearchService {

    @Autowired
    private TaxAffectationRepository taxAffectationRepository;
    @Autowired
    private TaxRepository taxRepository;

    private SearchTService<TaxAffectationEntity> searchTService;

    @Autowired
    private void initSearchService() {
        this.searchTService = new SearchTService<>(this.taxAffectationRepository);
    }

    public ResponsePageSearchT<TaxAffectationEntity> findAll(String query, int page) {
        return this.searchTService.findAll(new SearchDto(query, page), 10);
    }

    public TaxAffectationEntity findById(String taxAffectationCod) {
        return this.taxAffectationRepository.findById(taxAffectationCod).orElse(null);
    }

    public List<TaxAffectationEntity> findAllActive() {
        return this.taxAffectationRepository.findAllActive();
    }

    public ResponseWsDto findDataForm(String taxAffectationCod) {
        ResponseWsDto rpt = new ResponseWsDto();
        if (StringUtil.isNotEmpty(taxAffectationCod)) {
            rpt.AddResponseAdditional("taxAffectation", this.findById(taxAffectationCod));
        }
        rpt.AddResponseAdditional("taxList", this.taxRepository.findAllActive());
        return rpt;
    }
}
