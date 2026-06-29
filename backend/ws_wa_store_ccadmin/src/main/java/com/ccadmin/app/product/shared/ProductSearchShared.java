package com.ccadmin.app.product.shared;

import com.ccadmin.app.product.model.entity.ProductConfigEntity;
import com.ccadmin.app.product.service.ProductSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ProductSearchShared {

    @Autowired
    private ProductSearchService productSearchService;

    public ProductConfigEntity findConfigByIdAndStore(String ProductCod, String StoreCod){
        return this.productSearchService.findConfigByIdAndStore(ProductCod,StoreCod);
    }
}
