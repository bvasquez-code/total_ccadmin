package com.ccadmin.app.product.shared;

import com.ccadmin.app.product.model.entity.ProductInfoWarehouseEntity;
import com.ccadmin.app.product.model.entity.id.ProductInfoWarehouseId;
import com.ccadmin.app.product.service.ProductInfoWarehouseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductInfoWarehouseShared {

    @Autowired
    private ProductInfoWarehouseService productInfoWarehouseService;

    public ProductInfoWarehouseEntity findById(ProductInfoWarehouseId id)
    {
        return this.productInfoWarehouseService.findById(id);
    }

    public List<ProductInfoWarehouseEntity> saveAll(List<ProductInfoWarehouseEntity> list)
    {
        return this.productInfoWarehouseService.saveAll(list);
    }

    public ProductInfoWarehouseEntity save(ProductInfoWarehouseEntity productInfoWarehouse)
    {
        return this.productInfoWarehouseService.save(productInfoWarehouse);
    }

    public List<ProductInfoWarehouseEntity> findByStore(String StoreCod){
        return this.productInfoWarehouseService.findByStore(StoreCod);
    }

    public List<ProductInfoWarehouseEntity> findInfoWarehouse(String StoreCod,String ProductCod){
        return this.productInfoWarehouseService.findInfoWarehouse(StoreCod,ProductCod);
    }

}
