package com.ccadmin.app.product.shared;

import com.ccadmin.app.product.model.entity.ProductConfigEntity;
import com.ccadmin.app.product.service.ProductConfigSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ProductSearchShared {

    @Autowired
    private ProductConfigSearchService productConfigSearchService;

    public ProductConfigEntity findConfigByIdAndStore(String ProductCod, String StoreCod){
        return this.productConfigSearchService.findByIdAndStore(ProductCod,StoreCod);
    }
}
