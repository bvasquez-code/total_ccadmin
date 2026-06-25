package com.ccadmin.app.product.shared;

import com.ccadmin.app.product.model.entity.ProductSearchEntity;
import com.ccadmin.app.product.service.ProductFindCreateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductFindCreateShared {

    @Autowired
    private ProductFindCreateService productFindCreateService;

    public ProductSearchEntity save(String ProductCod, String StoreCod) {
        return this.productFindCreateService.save(ProductCod,StoreCod);
    }

    public List<ProductSearchEntity> generateSearch(String ProductCod){
        return this.productFindCreateService.generateSearch(ProductCod);
    }

}
