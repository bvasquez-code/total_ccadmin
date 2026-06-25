package com.ccadmin.app.product.shared;

import com.ccadmin.app.product.model.entity.ProductInfoEntity;
import com.ccadmin.app.product.model.entity.id.ProductInfoId;
import com.ccadmin.app.product.service.ProductInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductInfoShared {

    @Autowired
    private ProductInfoService productInfoService;

    public ProductInfoEntity findById(ProductInfoId id)
    {
        return this.productInfoService.findById(id);
    }

    public List<ProductInfoEntity> saveAll(List<ProductInfoEntity> list)
    {
        return this.productInfoService.saveAll(list);
    }

    public ProductInfoEntity save(ProductInfoEntity productInfo)
    {
        return this.productInfoService.save(productInfo);
    }
    public List<ProductInfoEntity> findAll(){
        return this.productInfoService.findAll();
    }

}
