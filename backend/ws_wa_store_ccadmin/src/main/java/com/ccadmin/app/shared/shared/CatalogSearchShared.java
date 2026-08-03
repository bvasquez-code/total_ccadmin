package com.ccadmin.app.shared.shared;

import java.util.List;

import com.ccadmin.app.shared.model.constants.BusinessConfigConstants;
import com.ccadmin.app.system.model.dto.IndicatorDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ccadmin.app.shared.service.CatalogSearchService;
import com.ccadmin.app.system.model.dto.DocumentTypeDto;
import com.ccadmin.app.system.model.dto.GenericCatalogDto;

@Service
public class CatalogSearchShared {

    @Autowired
    private CatalogSearchService catalogSearchService;

    public List<DocumentTypeDto> getSaleDocumentType() {
        return this.catalogSearchService.getSaleDocumentType();
    }

    public List<GenericCatalogDto> getGenericCatalog(String groupCod) {
        return this.catalogSearchService.getGenericCatalog(groupCod);
    }

    public List<GenericCatalogDto> getPaymentMethodType() {
        return this.catalogSearchService.getPaymentMethodType();
    }

    public IndicatorDto findIndicator(String groupCod, String configCod){
        return this.catalogSearchService.findIndicator(groupCod,configCod);
    }

    public IndicatorDto findIndicatorSystem(String configCod){
        return this.catalogSearchService.findIndicator(BusinessConfigConstants.GroupCod.SYSTEM_FUNCTIONALITY_ACTIVATOR,configCod);
    }

    public boolean isIndicatorSystemEnabled(String configCod) {
        IndicatorDto indicator = this.findIndicatorSystem(configCod);
        return indicator != null
                && indicator.Value != null
                && "S".equalsIgnoreCase(indicator.Value.trim());
    }
}
