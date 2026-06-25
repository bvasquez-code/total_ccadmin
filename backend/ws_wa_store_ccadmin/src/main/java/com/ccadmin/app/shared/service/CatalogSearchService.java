package com.ccadmin.app.shared.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ccadmin.app.system.model.dto.DocumentTypeDto;
import com.ccadmin.app.system.model.dto.GenericCatalogDto;

@Service
public class CatalogSearchService {

    @Autowired
    private BusinessConfigSearchService businessConfigSearchService;


    public List<DocumentTypeDto> getSaleDocumentType() {
        return this.businessConfigSearchService.findActivesByGroupCod("SalesDocumentType")
                .stream()
                .map(e -> new DocumentTypeDto(e.ConfigCod, e.ConfigVal))
                .toList();
    }

    public List<GenericCatalogDto> getGenericCatalog(String groupCod) {
        return this.businessConfigSearchService.findActivesByGroupCod(groupCod)
                .stream()
                .map(e -> new GenericCatalogDto(e.ConfigCod, e.ConfigVal))
                .toList();
    }

    public List<GenericCatalogDto> getPaymentMethodType() {
        return this.businessConfigSearchService.findActivesByGroupCod("PaymentMethodType")
                .stream()
                .map(e -> new GenericCatalogDto(e.ConfigCod, e.ConfigVal))
                .toList();
    }
}
