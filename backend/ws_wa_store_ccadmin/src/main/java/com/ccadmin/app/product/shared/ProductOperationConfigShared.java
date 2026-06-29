package com.ccadmin.app.product.shared;

import com.ccadmin.app.product.model.entity.ProductConfigEntity;
import com.ccadmin.app.product.model.entity.id.ProductConfigID;
import com.ccadmin.app.product.repository.ProductConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ProductOperationConfigShared {

    @Autowired
    private ProductConfigRepository productConfigRepository;

    public ProductConfigEntity findByProduct(String productCod, String storeCod) {
        ProductConfigEntity config = this.productConfigRepository.findById(new ProductConfigID(productCod, storeCod)).orElse(null);
        if (config == null) {
            config = this.productConfigRepository.findAnyByProductCod(productCod);
            if (config == null) {
                config = new ProductConfigEntity();
                config.ProductCod = productCod;
                config.StoreCod = storeCod;
            }
        }
        normalize(config);
        return config;
    }

    public ProductConfigEntity normalize(ProductConfigEntity config) {
        if (config.ProductUnitName == null || config.ProductUnitName.trim().isEmpty()) {
            config.ProductUnitName = "NIU";
        }
        if (config.ProductUnitFactor <= 0) {
            config.ProductUnitFactor = 1;
        }
        return config;
    }

    public void validateInternalQuantity(String productCod, int internalQuantity, int ProductUnitFactor) {
        if (ProductUnitFactor <= 0) {
            throw new IllegalArgumentException("Factor de operacion invalido para el producto " + productCod);
        }
        if (internalQuantity % ProductUnitFactor != 0) {
            throw new IllegalArgumentException(
                    "La cantidad del producto " + productCod + " no es compatible con el factor configurado " + ProductUnitFactor
            );
        }
    }
}
