package com.ccadmin.app.shared.shared;

import com.ccadmin.app.shared.model.dto.UrlDataDto;
import com.ccadmin.app.shared.service.BusinessConfigSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UrlSearchShared {

    @Autowired
    private BusinessConfigSearchService businessConfigSearchService;


    public UrlDataDto findUrlDtaSunat(String codUrl){

        var config = this.businessConfigSearchService.findByConfigCod("UrlServiciosSunat",codUrl);

        if(config == null){
            throw new IllegalArgumentException("URL no encontrada o sin configuración agregada.");
        }

        return UrlDataDto.buildForBusinessConfig(config);
    }

}
