package com.ccadmin.app.product.service;

import com.ccadmin.app.product.model.entity.ProductInfoEntity;
import com.ccadmin.app.product.model.entity.id.ProductInfoId;
import com.ccadmin.app.product.repository.ProductInfoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductInfoService {

    @Autowired
    private ProductInfoRepository productInfoRepository;

    public ProductInfoEntity findById(ProductInfoId id)
    {
        return this.productInfoRepository.findById(id).get();
    }

    public List<ProductInfoEntity> saveAll(List<ProductInfoEntity> list)
    {
        return this.productInfoRepository.saveAll(list);
    }

    public ProductInfoEntity save(ProductInfoEntity productInfo)
    {
        return this.productInfoRepository.save(productInfo);
    }

    public List<ProductInfoEntity> findAll(){
        return this.productInfoRepository.findAll();
    }
}
