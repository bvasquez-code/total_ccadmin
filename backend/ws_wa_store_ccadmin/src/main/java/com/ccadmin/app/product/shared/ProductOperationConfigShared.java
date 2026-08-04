package com.ccadmin.app.product.shared;

import com.ccadmin.app.product.model.entity.ProductConfigEntity;
import com.ccadmin.app.product.model.entity.ProductInfoEntity;
import com.ccadmin.app.product.model.entity.id.ProductConfigID;
import com.ccadmin.app.product.repository.ProductConfigRepository;
import com.ccadmin.app.product.repository.ProductInfoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ProductOperationConfigShared {

    @Autowired
    private ProductConfigRepository productConfigRepository;
    @Autowired
    private ProductInfoRepository productInfoRepository;

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
        if (config.IsDigital == null || config.IsDigital.trim().isEmpty()) {
            config.IsDigital = "N";
        } else {
            config.IsDigital = config.IsDigital.trim().toUpperCase();
        }
        return config;
    }

    public boolean isDigital(String productCod, String storeCod) {
        return this.isDigital(this.findByProduct(productCod, storeCod));
    }

    public boolean isDigital(ProductConfigEntity config) {
        return config != null && "S".equalsIgnoreCase(config.IsDigital);
    }

    public void validateDigitalIndicator(ProductConfigEntity config) {
        this.normalize(config);
        if (!"S".equals(config.IsDigital) && !"N".equals(config.IsDigital)) {
            throw new IllegalArgumentException("El indicador de producto digital debe ser S o N");
        }
    }

    public void validateDigitalConversion(
            String productCod,
            String storeCod,
            String targetIsDigital
    ) {
        if (!"S".equalsIgnoreCase(targetIsDigital)) {
            return;
        }

        ProductConfigEntity currentConfig = this.productConfigRepository.findForUpdate(productCod, storeCod);
        this.validateDigitalConversion(currentConfig, productCod, storeCod, targetIsDigital);
    }

    public void validateDigitalConversion(
            ProductConfigEntity currentConfig,
            String productCod,
            String storeCod,
            String targetIsDigital
    ) {
        if (!"S".equalsIgnoreCase(targetIsDigital)) {
            return;
        }
        if (currentConfig == null || this.isDigital(currentConfig)) {
            return;
        }

        ProductInfoEntity stock = this.productInfoRepository.findInfoStoreForUpdate(productCod, storeCod)
                .stream()
                .filter(this::hasStock)
                .findFirst()
                .orElse(null);
        if (stock != null) {
            throw new IllegalArgumentException(
                    "El producto " + productCod + " no puede convertirse en digital en el local " + storeCod
                            + " porque mantiene stock en la variante " + stock.Variant
                            + " (disponible: " + stock.NumDigitalStock
                            + ", fisico: " + stock.NumPhysicalStock
                            + ", no disponible: " + stock.NumUnavailableStock
                            + ", reservado: " + stock.NumReservedStock
                            + ", total: " + stock.NumTotalStock + ")"
            );
        }
    }

    private boolean hasStock(ProductInfoEntity stock) {
        return stock.NumDigitalStock != 0
                || stock.NumPhysicalStock != 0
                || stock.NumUnavailableStock != 0
                || stock.NumReservedStock != 0
                || stock.NumTotalStock != 0;
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
