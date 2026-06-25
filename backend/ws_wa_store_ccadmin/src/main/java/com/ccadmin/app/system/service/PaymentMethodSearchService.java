package com.ccadmin.app.system.service;

import com.ccadmin.app.shared.model.dto.ResponsePageSearchT;
import com.ccadmin.app.shared.model.dto.ResponseWsDto;
import com.ccadmin.app.shared.model.dto.SearchDto;
import com.ccadmin.app.shared.service.SearchTService;
import com.ccadmin.app.shared.shared.CatalogSearchShared;
import com.ccadmin.app.system.model.entity.PaymentMethodEntity;
import com.ccadmin.app.system.repository.PaymentMethodRepository;
import com.ccadmin.app.system.utility.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PaymentMethodSearchService {

    @Autowired
    private PaymentMethodRepository paymentMethodRepository;

    @Autowired
    private CatalogSearchShared catalogSearchShared;

    private SearchTService<PaymentMethodEntity> searchTService;

    @Autowired
    private void initSearchService() {
        this.searchTService = new SearchTService<>(this.paymentMethodRepository);
    }

    public ResponsePageSearchT<PaymentMethodEntity> findAll(String query, int page) {
        return this.searchTService.findAll(new SearchDto(query, page), 10);
    }

    public PaymentMethodEntity findById(String paymentMethodCod) {
        return this.paymentMethodRepository.findById(paymentMethodCod).orElse(null);
    }

    public List<PaymentMethodEntity> findAllActive() {
        return this.paymentMethodRepository.findAllActive();
    }

    public ResponseWsDto findDataForm(String paymentMethodCod) {
        ResponseWsDto rpt = new ResponseWsDto();

        if (StringUtil.isNotEmpty(paymentMethodCod)) {
            rpt.AddResponseAdditional("paymentMethod", this.findById(paymentMethodCod));
        }
        rpt.AddResponseAdditional("paymentMethodType", this.catalogSearchShared.getPaymentMethodType());

        return rpt;
    }
}
