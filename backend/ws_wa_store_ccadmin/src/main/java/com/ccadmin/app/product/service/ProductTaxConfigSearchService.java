package com.ccadmin.app.product.service;

import com.ccadmin.app.product.model.entity.ProductTaxConfigEntity;
import com.ccadmin.app.product.repository.ProductTaxConfigRepository;
import com.ccadmin.app.sale.repository.TaxAffectationRepository;
import com.ccadmin.app.sale.repository.TaxRepository;
import com.ccadmin.app.shared.model.dto.ResponseWsDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductTaxConfigSearchService {

    @Autowired
    private ProductTaxConfigRepository productTaxConfigRepository;
    @Autowired
    private TaxRepository taxRepository;
    @Autowired
    private TaxAffectationRepository taxAffectationRepository;

    public ProductTaxConfigEntity findById(Long productTaxConfigId) {
        return this.productTaxConfigRepository.findById(productTaxConfigId).orElse(null);
    }

    public List<ProductTaxConfigEntity> findByProductAndStore(String productCod, String storeCod) {
        return this.productTaxConfigRepository.findByProductAndStore(productCod, storeCod);
    }

    public List<ProductTaxConfigEntity> findActiveByProductAndStore(String productCod, String storeCod) {
        return this.productTaxConfigRepository.findActiveByProductAndStore(productCod, storeCod);
    }

    public ResponseWsDto findDataForm(String productCod, String storeCod) {
        ResponseWsDto rpt = new ResponseWsDto();
        rpt.AddResponseAdditional("productTaxConfigList", this.findByProductAndStore(productCod, storeCod));
        rpt.AddResponseAdditional("taxList", this.taxRepository.findAllActive());
        rpt.AddResponseAdditional("taxAffectationList", this.taxAffectationRepository.findAllActive());
        return rpt;
    }
}
