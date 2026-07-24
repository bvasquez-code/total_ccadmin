package com.ccadmin.app.product.service;

import com.ccadmin.app.product.model.entity.ProductConfigEntity;
import com.ccadmin.app.product.model.entity.id.ProductConfigID;
import com.ccadmin.app.product.repository.ProductConfigRepository;
import com.ccadmin.app.product.repository.ProductRepository;
import com.ccadmin.app.product.repository.ProductTaxConfigRepository;
import com.ccadmin.app.sale.repository.TaxAffectationRepository;
import com.ccadmin.app.sale.repository.TaxRepository;
import com.ccadmin.app.shared.model.dto.ResponseWsDto;
import com.ccadmin.app.shared.service.SessionService;
import com.ccadmin.app.shared.shared.CatalogSearchShared;
import com.ccadmin.app.store.shared.StoreShared;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ProductConfigSearchService extends SessionService {

    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private ProductConfigRepository productConfigRepository;
    @Autowired
    private ProductTaxConfigRepository productTaxConfigRepository;
    @Autowired
    private TaxRepository taxRepository;
    @Autowired
    private TaxAffectationRepository taxAffectationRepository;
    @Autowired
    private StoreShared storeShared;
    @Autowired
    private CatalogSearchShared catalogSearchShared;

    public ResponseWsDto findDataConfigForm(String ProductCod, String StoreCod) {
        ResponseWsDto response = new ResponseWsDto();
        String storeCod = StoreCod != null && !StoreCod.isEmpty() ? StoreCod : getStoreCod();
        ProductConfigEntity config = this.productConfigRepository.findById(new ProductConfigID(ProductCod, storeCod))
                .orElseGet(() -> this.productConfigRepository.findAnyByProductCod(ProductCod));
        if (config == null) {
            config = new ProductConfigEntity();
            config.ProductCod = ProductCod;
            config.StoreCod = storeCod;
        }

        response.AddResponseAdditional("product", this.productRepository.findById(ProductCod).orElseThrow());
        response.AddResponseAdditional("config", config);
        response.AddResponseAdditional(
                "productTaxConfigList",
                this.productTaxConfigRepository.findByProductAndStore(ProductCod, storeCod));
        response.AddResponseAdditional("taxList", this.taxRepository.findAllActive());
        response.AddResponseAdditional("taxAffectationList", this.taxAffectationRepository.findAllActive());
        response.AddResponseAdditional("store", this.storeShared.findById(storeCod));
        response.AddResponseAdditional("storeList", this.storeShared.findAll());
        response.AddResponseAdditional(
                "indDetailedTaxIndicator",
                this.catalogSearchShared.findIndicator("ActiTaxCalcFunctionalities", "IndDetailedTaxIndicator"));
        return response;
    }

    public ProductConfigEntity findByIdAndStore(String ProductCod, String StoreCod) {
        return this.productConfigRepository.findById(new ProductConfigID(ProductCod, StoreCod)).orElse(null);
    }
}
