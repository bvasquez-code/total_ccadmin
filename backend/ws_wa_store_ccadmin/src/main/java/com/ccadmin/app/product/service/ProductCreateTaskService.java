package com.ccadmin.app.product.service;

import com.ccadmin.app.shared.service.IGenericTaskService;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class ProductCreateTaskService implements IGenericTaskService {

    private ProductFindCreateService productFindCreateService;
    private List<String> productCodList;

    public ProductCreateTaskService(ProductFindCreateService productFindCreateService,List<String> productCodList){
        this.productCodList = productCodList;
        this.productFindCreateService = productFindCreateService;
    }
    @Override
    public void execute() {
        for(String productCod : this.productCodList){
            log.info("INI-GENERANDO INFO DE BUSQUEDA : {}",productCod);
            this.productFindCreateService.generateSearch(productCod);
            log.info("FIN-GENERANDO INFO DE BUSQUEDA  {}",productCod);
        }
    }
}
