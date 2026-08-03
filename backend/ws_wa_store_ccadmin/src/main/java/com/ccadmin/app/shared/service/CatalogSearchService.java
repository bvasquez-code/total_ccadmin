package com.ccadmin.app.shared.service;

import java.util.List;

import com.ccadmin.app.shared.model.constants.BusinessConfigConstants;
import com.ccadmin.app.shared.model.dto.ConfigAutomaticProcessThreads;
import com.ccadmin.app.shared.model.entity.BusinessConfigEntity;
import com.ccadmin.app.shared.model.myconst.StatusConst;
import com.ccadmin.app.system.model.dto.IndicatorDto;
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


    public IndicatorDto findIndicator(String groupCod, String configCod){
        BusinessConfigEntity businessConfig = this.businessConfigSearchService.findByConfigCod(groupCod,configCod);
        if (businessConfig == null || !StatusConst.ACTIVE.equals(businessConfig.Status)) {
            return new IndicatorDto(configCod, "N");
        }
        return new IndicatorDto(businessConfig.ConfigCod,businessConfig.ConfigVal);
    }

    public ConfigAutomaticProcessThreads findConfigAutomaticProcessThreads(String configCod){
        BusinessConfigEntity businessConfig = this.businessConfigSearchService.findByConfigCod(
                BusinessConfigConstants.GroupCod.CONFIG_AUTOMATIC_PROCESS_THREADS,
                configCod
        );
        if (businessConfig == null || !StatusConst.ACTIVE.equals(businessConfig.Status)) {
            return null;
        }
        return new ConfigAutomaticProcessThreads(
                businessConfig.ConfigCod,
                businessConfig.ConfigDesc,
                Long.parseLong(businessConfig.Num1Config.toString()),
                Long.parseLong(businessConfig.Num2Config.toString()),
                businessConfig.Num3Config,
                businessConfig.Num4Config
        );
    }
}
