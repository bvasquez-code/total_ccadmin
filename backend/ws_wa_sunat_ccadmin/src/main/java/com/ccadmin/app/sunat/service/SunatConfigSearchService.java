package com.ccadmin.app.sunat.service;

import com.ccadmin.app.shared.model.dto.ResponsePageSearchT;
import com.ccadmin.app.shared.model.dto.ResponseWsDto;
import com.ccadmin.app.shared.model.dto.SearchDto;
import com.ccadmin.app.shared.service.SearchTService;
import com.ccadmin.app.sunat.model.entity.SunatConfigEntity;
import com.ccadmin.app.sunat.model.dto.SunatInitializationFormDto;
import com.ccadmin.app.sunat.model.constants.SunatConfigDefaults;
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

    public SunatInitializationFormDto findInitializationForm() {
        SunatInitializationFormDto form = new SunatInitializationFormDto();
        form.BetaEndpoint = SunatConfigDefaults.BETA_ENDPOINT;
        form.ProductionEndpoint = SunatConfigDefaults.PRODUCTION_ENDPOINT;
        form.GuideEndpoint = SunatConfigDefaults.GUIDE_ENDPOINT;
        form.GuideTokenEndpoint = SunatConfigDefaults.GUIDE_TOKEN_ENDPOINT;
        SunatConfigEntity active = sunatConfigRepository.findActiveConfig().orElse(null);
        SunatConfigEntity production = sunatConfigRepository.findById(SunatConfigDefaults.PRODUCTION_CODE).orElse(null);
        if (active != null) form.Mode = active.Environment;
        if (production != null) {
            form.IssuerRuc = production.IssuerRuc;
            form.SolUser = production.SolUser;
            form.HasSolPassword = present(production.SolPassword);
            form.HasCertificate = present(production.CertificatePath);
            form.HasCertificatePassword = present(production.CertificatePassword);
            form.GuideClientId = production.GuideClientId;
            form.HasGuideClientSecret = present(production.GuideClientSecret);
            form.GuideEnabled = present(production.GuideClientId) && form.HasGuideClientSecret;
        }
        return form;
    }

    private boolean present(String value) { return value != null && !value.isBlank(); }

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
