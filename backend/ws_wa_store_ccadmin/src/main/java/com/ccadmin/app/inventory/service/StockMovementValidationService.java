package com.ccadmin.app.inventory.service;

import com.ccadmin.app.inventory.model.constants.StockMovementConstants;
import com.ccadmin.app.product.shared.ProductOperationConfigShared;
import com.ccadmin.app.shared.repository.BusinessConfigRepository;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

@Service
public class StockMovementValidationService {
    private final BusinessConfigRepository businessConfigRepository;
    private final ProductOperationConfigShared productOperationConfigShared;

    public StockMovementValidationService(
            BusinessConfigRepository businessConfigRepository,
            ProductOperationConfigShared productOperationConfigShared
    ) {
        this.businessConfigRepository = businessConfigRepository;
        this.productOperationConfigShared = productOperationConfigShared;
    }

    public void requireReason(Integer groupId, String configCod, String fieldName) {
        if (configCod == null || configCod.isBlank()
                || businessConfigRepository.countActiveByGroupIdAndConfigCod(groupId, configCod) == 0) {
            throw new IllegalArgumentException(fieldName + " no pertenece al grupo de configuracion requerido");
        }
    }

    public void validateResolution(String resolutionType, String reasonCode,
                                   String observation, java.util.Date nextReviewDate) {
        if (StockMovementConstants.RESOLUTION_RELEASE.equals(resolutionType)) {
            requireReason(11, reasonCode, "El motivo de liberacion");
            return;
        }
        if (StockMovementConstants.RESOLUTION_WITHDRAW.equals(resolutionType)
                || StockMovementConstants.RESOLUTION_DESTROY.equals(resolutionType)) {
            requireReason(12, reasonCode, "El motivo de retiro definitivo");
            return;
        }
        if (StockMovementConstants.RESOLUTION_MAINTAIN.equals(resolutionType)) {
            if (observation == null || observation.isBlank() || nextReviewDate == null) {
                throw new IllegalArgumentException(
                        "Mantener no disponible requiere observacion y fecha de proxima revision"
                );
            }
            return;
        }
        throw new IllegalArgumentException("Tipo de resolucion no soportado");
    }

    public int positive(Integer value, String fieldName) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(fieldName + " debe ser mayor a cero");
        }
        return value;
    }

    public void requirePhysicalProducts(Collection<String> productCodes, String storeCod) {
        if (productCodes == null || productCodes.isEmpty()) {
            return;
        }
        Set<String> uniqueProductCodes = new LinkedHashSet<>();
        for (String productCode : productCodes) {
            if (productCode != null && !productCode.isBlank()) {
                uniqueProductCodes.add(productCode.trim());
            }
        }
        for (String productCode : uniqueProductCodes) {
            if (productOperationConfigShared.isDigital(productCode, storeCod)) {
                throw new IllegalArgumentException(
                        "El producto " + productCode
                                + " es digital y no puede utilizarse en movimientos de stock"
                );
            }
        }
    }
}
