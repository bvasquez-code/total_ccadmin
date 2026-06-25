package com.ccadmin.app.product.shared;

import com.ccadmin.app.product.model.entity.ProductEntity;
import com.ccadmin.app.product.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductShared {

    @Autowired
    private ProductService productService;

    public ProductEntity findById(String ProductCod)
    {
        return this.productService.findById(ProductCod);
    }

    public List<ProductEntity> findAll(){
        return this.productService.findAll();
    }

    public List<ProductEntity> findAllById(List<String> ProductCodList){
        return this.productService.findAllById(ProductCodList);
    }

    public List<ProductEntity> findByBrandCod(String BrandCod){
        return this.productService.findByBrandCod(BrandCod);
    }
    public List<ProductEntity> findByCategoryCod(String CategoryCod){
        return this.productService.findByCategoryCod(CategoryCod);
    }
}
