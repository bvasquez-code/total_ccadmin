package com.ccadmin.app.sunat.service;

import com.ccadmin.app.shared.model.dto.ResponsePageSearchT;
import com.ccadmin.app.shared.model.dto.ResponseWsDto;
import com.ccadmin.app.shared.model.dto.SearchDto;
import com.ccadmin.app.shared.service.SearchTService;
import com.ccadmin.app.sunat.model.entity.SunatConfigEntity;
import com.ccadmin.app.sunat.repository.SunatConfigRepository;
import com.ccadmin.app.system.utility.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SunatConfigSearchService {

    @Autowired
    private SunatConfigRepository sunatConfigRepository;

    private SearchTService<SunatConfigEntity> searchTService;

    @Autowired
    private void initSearchService() {
        this.searchTService = new SearchTService<>(this.sunatConfigRepository);
    }

    public ResponsePageSearchT<SunatConfigEntity> findAll(String query, int page) {
        return this.searchTService.findAll(new SearchDto(query, page), 10);
    }

    public SunatConfigEntity findById(String sunatConfigCod) {
        return this.sunatConfigRepository.findById(sunatConfigCod).orElse(null);
    }

    public SunatConfigEntity findActive() {
        return this.sunatConfigRepository.findActiveConfig()
                .orElseThrow(() -> new IllegalArgumentException("No existe configuracion SUNAT activa"));
    }

    public ResponseWsDto findDataForm(String sunatConfigCod) {
        ResponseWsDto rpt = new ResponseWsDto();
        if (StringUtil.isNotEmpty(sunatConfigCod)) {
            rpt.AddResponseAdditional("sunatConfig", this.findById(sunatConfigCod));
        }
        rpt.AddResponseAdditional("environment", new String[]{"BETA", "PRODUCCION"});
        rpt.AddResponseAdditional("certificateType", new String[]{"P12", "PFX", "JKS"});
        return rpt;
    }
}
