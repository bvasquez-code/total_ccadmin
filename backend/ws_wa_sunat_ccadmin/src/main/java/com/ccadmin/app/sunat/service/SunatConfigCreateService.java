package com.ccadmin.app.sunat.service;

import com.ccadmin.app.shared.service.SessionService;
import com.ccadmin.app.sunat.model.entity.SunatConfigEntity;
import com.ccadmin.app.sunat.repository.SunatConfigRepository;
import com.ccadmin.app.sunat.utility.SunatCodeUtil;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SunatConfigCreateService extends SessionService {

    @Autowired
    private SunatConfigRepository sunatConfigRepository;

    @Transactional
    public SunatConfigEntity save(SunatConfigEntity sunatConfig) {
        if (sunatConfig == null) {
            throw new IllegalArgumentException("Configuracion SUNAT requerida");
        }
        if (sunatConfig.SunatConfigCod == null || sunatConfig.SunatConfigCod.isBlank()) {
            sunatConfig.SunatConfigCod = SunatCodeUtil.newCode("SC");
        }
        sunatConfig.normalizeFlags();
        sunatConfig.validate();
        boolean isNew = !this.sunatConfigRepository.existsById(sunatConfig.SunatConfigCod);
        if ("S".equals(sunatConfig.ActiveConfig)) {
            this.sunatConfigRepository.deactivateActiveConfigs(this.getUserCod());
        }
        sunatConfig.addSession(this.getUserCod(), isNew);
        return this.sunatConfigRepository.save(sunatConfig);
    }

    @Transactional
    public SunatConfigEntity activate(SunatConfigEntity request) {
        SunatConfigEntity sunatConfig = this.sunatConfigRepository.findById(request.SunatConfigCod)
                .orElseThrow(() -> new IllegalArgumentException("Configuracion SUNAT no encontrada"));
        sunatConfig.validate();
        this.sunatConfigRepository.deactivateActiveConfigs(this.getUserCod());
        sunatConfig.activate(this.getUserCod());
        return this.sunatConfigRepository.save(sunatConfig);
    }

    public SunatConfigEntity enable(SunatConfigEntity request) {
        SunatConfigEntity sunatConfig = this.sunatConfigRepository.findById(request.SunatConfigCod)
                .orElseThrow(() -> new IllegalArgumentException("Configuracion SUNAT no encontrada"));
        sunatConfig.active(this.getUserCod());
        return this.sunatConfigRepository.save(sunatConfig);
    }

    public SunatConfigEntity disable(SunatConfigEntity request) {
        SunatConfigEntity sunatConfig = this.sunatConfigRepository.findById(request.SunatConfigCod)
                .orElseThrow(() -> new IllegalArgumentException("Configuracion SUNAT no encontrada"));
        sunatConfig.deactivate(this.getUserCod());
        return this.sunatConfigRepository.save(sunatConfig);
    }
}
