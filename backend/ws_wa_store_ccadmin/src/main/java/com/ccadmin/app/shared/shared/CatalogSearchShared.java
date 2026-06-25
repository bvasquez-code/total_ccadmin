package com.ccadmin.app.shared.shared;

import java.util.List;

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
}
