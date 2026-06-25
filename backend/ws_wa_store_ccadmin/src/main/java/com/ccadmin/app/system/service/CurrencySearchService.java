package com.ccadmin.app.system.service;

import com.ccadmin.app.shared.model.dto.ResponsePageSearchT;
import com.ccadmin.app.shared.model.dto.ResponseWsDto;
import com.ccadmin.app.shared.model.dto.SearchDto;
import com.ccadmin.app.shared.service.SearchTService;
import com.ccadmin.app.system.model.entity.CurrencyEntity;
import com.ccadmin.app.system.repository.CurrencyRepository;
import com.ccadmin.app.system.utility.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CurrencySearchService {

    @Autowired
    private CurrencyRepository currencyRepository;


    private SearchTService<CurrencyEntity> searchTService;

    @Autowired
    private void initSearchService() {
        this.searchTService = new SearchTService<>(this.currencyRepository);
    }

    public ResponsePageSearchT<CurrencyEntity> findAll(String query, int page) {
        return this.searchTService.findAll(new SearchDto(query, page), 10);
    }

    public CurrencyEntity findById(String currencyCod) {
        return this.currencyRepository.findById(currencyCod).orElse(null);
    }

    public CurrencyEntity findCurrencySystem() {
        return this.currencyRepository.findCurrencySystem();
    }

    public List<CurrencyEntity> findAllActive() {
        return this.currencyRepository.findAllActive();
    }

    public ResponseWsDto findDataForm(String currencyCod) {
        ResponseWsDto rpt = new ResponseWsDto();

        if (StringUtil.isNotEmpty(currencyCod)) {
            rpt.AddResponseAdditional("currency", this.findById(currencyCod));
        }

        rpt.AddResponseAdditional("currencySystem", this.findCurrencySystem());
        return rpt;
    }
}
