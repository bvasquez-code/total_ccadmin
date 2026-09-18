package com.ccadmin.app.report.service;

import com.ccadmin.app.shared.model.dto.ResponseWsDto;
import com.ccadmin.app.store.repository.StoreRepository;
import com.ccadmin.app.system.repository.CurrencyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportFormSearchService {
    private final StoreRepository storeRepository;
    private final CurrencyRepository currencyRepository;

    public ReportFormSearchService(StoreRepository storeRepository, CurrencyRepository currencyRepository) {
        this.storeRepository = storeRepository;
        this.currencyRepository = currencyRepository;
    }

    @Transactional(readOnly = true)
    public ResponseWsDto findDataForm() {
        ResponseWsDto response = new ResponseWsDto();
        response.AddResponseAdditional("storeList", storeRepository.findAllActive());
        response.AddResponseAdditional("currencyList", currencyRepository.findAllActive());
        return response;
    }
}
