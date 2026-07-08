package com.ccadmin.app.product.service;

import com.ccadmin.app.product.model.dto.ProductTaxConfigRegisterDto;
import com.ccadmin.app.product.model.entity.ProductTaxConfigEntity;
import com.ccadmin.app.product.model.entity.id.ProductConfigID;
import com.ccadmin.app.product.repository.ProductConfigRepository;
import com.ccadmin.app.product.repository.ProductTaxConfigRepository;
import com.ccadmin.app.sale.model.entity.TaxAffectationEntity;
import com.ccadmin.app.sale.model.entity.TaxEntity;
import com.ccadmin.app.sale.repository.TaxAffectationRepository;
import com.ccadmin.app.sale.repository.TaxRepository;
import com.ccadmin.app.shared.service.SessionService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ProductTaxConfigCreateService extends SessionService {

    private static final BigDecimal STANDARD_IGV_RATE = new BigDecimal("18.0000");

    @Autowired
    private ProductTaxConfigRepository productTaxConfigRepository;
    @Autowired
    private ProductConfigRepository productConfigRepository;
    @Autowired
    private TaxRepository taxRepository;
    @Autowired
    private TaxAffectationRepository taxAffectationRepository;

    @Transactional
    public ProductTaxConfigRegisterDto saveAllByProductStore(ProductTaxConfigRegisterDto request) {
        validateRequest(request);

        List<ProductTaxConfigEntity> currentList = this.productTaxConfigRepository
                .findByProductAndStore(request.ProductCod, request.StoreCod);

        normalizeAndValidateList(request.TaxConfigList, request.ProductCod, request.StoreCod);
        reuseCurrentActiveRows(currentList, request.TaxConfigList);
        inactiveMissingCurrentRows(currentList, request.TaxConfigList);

        List<ProductTaxConfigEntity> savedList = new ArrayList<>();
        for (ProductTaxConfigEntity item : request.TaxConfigList) {
            prepareAudit(item);
            savedList.add(this.productTaxConfigRepository.save(item));
        }
        request.TaxConfigList = savedList;
        return request;
    }

    public ProductTaxConfigEntity save(ProductTaxConfigEntity config) {
        if (config == null) {
            throw new IllegalArgumentException("Configuracion tributaria requerida");
        }
        validateProductConfigExists(config.ProductCod, config.StoreCod);
        normalizeAndValidate(config);
        validateAgainstStoredActiveRows(config);
        prepareAudit(config);
        return this.productTaxConfigRepository.save(config);
    }

    public ProductTaxConfigEntity disable(ProductTaxConfigEntity request) {
        ProductTaxConfigEntity config = this.productTaxConfigRepository.findById(request.ProductTaxConfigId)
                .orElseThrow(() -> new IllegalArgumentException("Configuracion tributaria no encontrada"));
        config.inactive(this.getUserCod());
        return this.productTaxConfigRepository.save(config);
    }

    public ProductTaxConfigEntity ensureDefaultMainTax(String productCod, String storeCod) {
        validateProductConfigExists(productCod, storeCod);
        if (!this.productTaxConfigRepository.findActiveByProductAndStore(productCod, storeCod).isEmpty()) {
            return null;
        }
        ProductTaxConfigEntity config = new ProductTaxConfigEntity();
        config.ProductCod = productCod;
        config.StoreCod = storeCod;
        config.TaxCod = "1000";
        config.TaxAffectationCod = "10";
        config.IsMainTax = "S";
        config.TaxRateValue = new BigDecimal("18.0000");
        config.FixedUnitAmount = BigDecimal.ZERO;
        config.TaxCalculationType = "P";
        config.IsInformative = "N";
        config.CalculationOrder = 20;
        normalizeAndValidate(config);
        config.addSessionCreate(this.getUserCod());
        return this.productTaxConfigRepository.save(config);
    }

    private void validateRequest(ProductTaxConfigRegisterDto request) {
        if (request == null) {
            throw new IllegalArgumentException("Configuracion tributaria requerida");
        }
        if (request.ProductCod == null || request.ProductCod.isBlank()) {
            throw new IllegalArgumentException("Producto requerido");
        }
        if (request.StoreCod == null || request.StoreCod.isBlank()) {
            throw new IllegalArgumentException("Tienda requerida");
        }
        validateProductConfigExists(request.ProductCod, request.StoreCod);
        if (request.TaxConfigList == null || request.TaxConfigList.isEmpty()) {
            throw new IllegalArgumentException("Debe registrar configuracion tributaria principal");
        }
    }

    private void validateProductConfigExists(String productCod, String storeCod) {
        if (productCod == null || storeCod == null
                || !this.productConfigRepository.existsById(new ProductConfigID(productCod, storeCod))) {
            throw new IllegalArgumentException("Configuracion de producto/local no existe");
        }
    }

    private void normalizeAndValidateList(List<ProductTaxConfigEntity> configList, String productCod, String storeCod) {
        int mainCount = 0;
        Set<String> activeTaxCodSet = new HashSet<>();
        String mainAffectationCod = null;

        for (ProductTaxConfigEntity item : configList) {
            item.ProductCod = productCod;
            item.StoreCod = storeCod;
            normalizeAndValidate(item);
            if (!isActive(item)) {
                continue;
            }
            if (!activeTaxCodSet.add(item.TaxCod)) {
                throw new IllegalArgumentException("No se puede duplicar el mismo tributo activo para el producto/local");
            }
            if ("S".equals(item.IsMainTax)) {
                mainCount++;
                mainAffectationCod = item.TaxAffectationCod;
            } else if (this.taxAffectationRepository.countActiveByTaxCod(item.TaxCod) > 0) {
                throw new IllegalArgumentException("Los tributos de afectacion IGV solo pueden configurarse como principal");
            }
        }

        if (mainCount != 1) {
            throw new IllegalArgumentException("Producto/local debe tener una sola afectacion principal activa");
        }
        if (!"10".equals(mainAffectationCod) && activeTaxCodSet.contains("1000")) {
            throw new IllegalArgumentException("Producto exonerado, inafecto o exportacion no puede tener IGV real calculado");
        }
    }

    private void normalizeAndValidate(ProductTaxConfigEntity config) {
        TaxEntity tax = this.taxRepository.findById(config.TaxCod)
                .orElseThrow(() -> new IllegalArgumentException("Tributo no existe"));

        config.TaxCalculationType = tax.TaxCalculationType;
        config.IsInformative = tax.IsInformative;
        if (config.TaxRateValue == null) {
            config.TaxRateValue = tax.TaxRateValue;
        }
        if (config.FixedUnitAmount == null) {
            config.FixedUnitAmount = tax.FixedUnitAmount;
        }
        if (config.CalculationOrder <= 0) {
            config.CalculationOrder = tax.CalculationOrder;
        }
        if (config.Status == null || config.Status.isBlank()) {
            config.Status = "A";
        }
        if (config.TaxAffectationCod != null && config.TaxAffectationCod.isBlank()) {
            config.TaxAffectationCod = null;
        }

        if ("S".equals(config.IsMainTax)) {
            TaxAffectationEntity affectation = this.taxAffectationRepository
                    .findActiveByCodeAndTax(config.TaxAffectationCod, config.TaxCod);
            if (affectation == null) {
                throw new IllegalArgumentException("Combinacion de tributo y afectacion no permitida");
            }
        } else if (config.TaxAffectationCod != null && !config.TaxAffectationCod.isBlank()) {
            TaxAffectationEntity affectation = this.taxAffectationRepository
                    .findActiveByCodeAndTax(config.TaxAffectationCod, config.TaxCod);
            if (affectation == null) {
                throw new IllegalArgumentException("Combinacion de tributo y afectacion no permitida");
            }
        }

        if ("1000".equals(config.TaxCod)) {
            config.TaxRateValue = STANDARD_IGV_RATE;
        }
        if ("F".equals(config.TaxCalculationType)) {
            config.TaxRateValue = BigDecimal.ZERO;
        }
        config.validate();
    }

    private void validateAgainstStoredActiveRows(ProductTaxConfigEntity config) {
        if (!isActive(config)) {
            return;
        }
        if (this.productTaxConfigRepository.countActiveTax(
                config.ProductCod, config.StoreCod, config.TaxCod, config.ProductTaxConfigId) > 0) {
            throw new IllegalArgumentException("No se puede duplicar el mismo tributo activo para el producto/local");
        }
        if ("S".equals(config.IsMainTax) && this.productTaxConfigRepository.countActiveMainTax(
                config.ProductCod, config.StoreCod, config.ProductTaxConfigId) > 0) {
            throw new IllegalArgumentException("Producto/local ya tiene una afectacion principal activa");
        }
    }

    private void inactiveMissingCurrentRows(List<ProductTaxConfigEntity> currentList, List<ProductTaxConfigEntity> requestList) {
        Set<Long> requestIdSet = new HashSet<>();
        for (ProductTaxConfigEntity item : requestList) {
            if (item.ProductTaxConfigId != null) {
                requestIdSet.add(item.ProductTaxConfigId);
            }
        }
        for (ProductTaxConfigEntity current : currentList) {
            if (isActive(current) && !requestIdSet.contains(current.ProductTaxConfigId)) {
                current.inactive(this.getUserCod());
                this.productTaxConfigRepository.save(current);
            }
        }
    }

    private void reuseCurrentActiveRows(List<ProductTaxConfigEntity> currentList, List<ProductTaxConfigEntity> requestList) {
        for (ProductTaxConfigEntity item : requestList) {
            if (!isActive(item) || item.ProductTaxConfigId != null) {
                continue;
            }
            if ("S".equals(item.IsMainTax)) {
                currentList.stream()
                        .filter(this::isActive)
                        .filter(current -> "S".equals(current.IsMainTax))
                        .findFirst()
                        .ifPresent(current -> item.ProductTaxConfigId = current.ProductTaxConfigId);
            }
            if (item.ProductTaxConfigId != null) {
                continue;
            }
            currentList.stream()
                    .filter(this::isActive)
                    .filter(current -> current.TaxCod.equals(item.TaxCod))
                    .findFirst()
                    .ifPresent(current -> item.ProductTaxConfigId = current.ProductTaxConfigId);
        }
    }

    private void prepareAudit(ProductTaxConfigEntity config) {
        if (config.ProductTaxConfigId == null) {
            config.addSessionCreate(this.getUserCod());
            return;
        }
        ProductTaxConfigEntity current = this.productTaxConfigRepository.findById(config.ProductTaxConfigId)
                .orElseThrow(() -> new IllegalArgumentException("Configuracion tributaria no encontrada"));
        if (!current.ProductCod.equals(config.ProductCod) || !current.StoreCod.equals(config.StoreCod)) {
            throw new IllegalArgumentException("Configuracion tributaria no pertenece al producto/local");
        }
        config.CreationUser = current.CreationUser;
        config.CreationDate = current.CreationDate;
        config.addSessionModify(this.getUserCod());
    }

    private boolean isActive(ProductTaxConfigEntity config) {
        return config.Status == null || config.Status.isBlank() || "A".equals(config.Status);
    }
}
