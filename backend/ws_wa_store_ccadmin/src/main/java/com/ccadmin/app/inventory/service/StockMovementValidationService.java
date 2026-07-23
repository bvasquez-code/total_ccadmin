package com.ccadmin.app.inventory.service;

import com.ccadmin.app.inventory.model.constants.StockMovementConstants;
import com.ccadmin.app.shared.repository.BusinessConfigRepository;
import org.springframework.stereotype.Service;

@Service
public class StockMovementValidationService {
    private final BusinessConfigRepository businessConfigRepository;

    public StockMovementValidationService(BusinessConfigRepository businessConfigRepository) {
        this.businessConfigRepository = businessConfigRepository;
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
}
