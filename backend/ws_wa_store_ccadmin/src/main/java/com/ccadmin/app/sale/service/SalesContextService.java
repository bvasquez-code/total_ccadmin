package com.ccadmin.app.sale.service;

import com.ccadmin.app.sale.model.dto.SalesContextDto;
import com.ccadmin.app.shared.model.constants.AuditUserConstants;
import com.ccadmin.app.shared.service.SessionService;
import org.springframework.stereotype.Service;

@Service
public class SalesContextService extends SessionService {

    public SalesContextDto getInternalContext() {
        return new SalesContextDto(
                getStoreCod(),
                getUserCod(),
                getCashSessionID()
        );
    }

    public SalesContextDto getWebContext(String StoreCod) {
        if (StoreCod == null || StoreCod.isBlank()) {
            throw new IllegalArgumentException("La tienda de la venta web es obligatoria");
        }

        return new SalesContextDto(
                StoreCod,
                AuditUserConstants.USER_WEB,
                null
        );
    }
}
