package com.ccadmin.app.sunat.service;

import com.ccadmin.app.shared.service.SessionService;
import com.ccadmin.app.sunat.model.entity.SunatConfigEntity;
import com.ccadmin.app.sunat.model.constants.SunatConfigDefaults;
import com.ccadmin.app.sunat.model.dto.SunatInitializationRequestDto;
import com.ccadmin.app.sunat.model.dto.SunatInitializationFormDto;
import com.ccadmin.app.sunat.repository.SunatConfigRepository;
import com.ccadmin.app.sunat.utility.SunatCodeUtil;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class SunatConfigCreateService extends SessionService {

    @Autowired
    private SunatConfigRepository sunatConfigRepository;

    @Autowired
    private SunatCertificateService sunatCertificateService;

    @Autowired
    private SunatConfigSearchService sunatConfigSearchService;

    @Transactional
    public SunatConfigEntity save(SunatConfigEntity sunatConfig) {
        return saveConfiguration(sunatConfig, this.getUserCod());
    }

    private SunatConfigEntity saveConfiguration(SunatConfigEntity sunatConfig, String userCod) {
        if (sunatConfig == null) {
            throw new IllegalArgumentException("Configuracion SUNAT requerida");
        }
        if (sunatConfig.SunatConfigCod == null || sunatConfig.SunatConfigCod.isBlank()) {
            sunatConfig.SunatConfigCod = SunatCodeUtil.newCode("SC");
        }
        sunatConfig.normalizeFlags();
        sunatConfig.validate();
        SunatConfigEntity previous = this.sunatConfigRepository.findById(sunatConfig.SunatConfigCod).orElse(null);
        boolean isNew = previous == null;
        if (!isNew) {
            sunatConfig.CreationUser = previous.CreationUser;
            sunatConfig.CreationDate = previous.CreationDate;
        }
        if ("S".equals(sunatConfig.ActiveConfig)) {
            this.sunatConfigRepository.deactivateActiveConfigs(userCod);
        }
        sunatConfig.addSession(userCod, isNew);
        return this.sunatConfigRepository.save(sunatConfig);
    }

    @Transactional
    public SunatInitializationFormDto configureInitialization(SunatInitializationRequestDto request,
                                                              MultipartFile certificate) {
        if (request == null || !java.util.Set.of("NONE", "BETA", "PRODUCCION").contains(request.Mode == null ? "" : request.Mode)) {
            throw new IllegalArgumentException("Seleccione cómo desea usar SUNAT");
        }
        String userCod = request.AuditUserCod == null || request.AuditUserCod.isBlank() ? getUserCod() : request.AuditUserCod;
        if ("NONE".equals(request.Mode)) {
            sunatConfigRepository.deactivateActiveConfigs(userCod);
            return sunatConfigSearchService.findInitializationForm();
        }
        SunatConfigEntity config = SunatConfigDefaults.create(request.Mode);
        config.IssuerRuc = request.IssuerRuc;
        SunatConfigEntity previous = sunatConfigRepository.findById(config.SunatConfigCod).orElse(null);
        String uploadedPath = null;
        try {
            if ("PRODUCCION".equals(request.Mode)) {
                config.SolUser = request.SolUser == null ? "" : request.SolUser.trim();
                boolean sameIssuer = previous != null && config.IssuerRuc != null && config.IssuerRuc.equals(previous.IssuerRuc);
                config.SolPassword = valueOrExisting(request.SolPassword, sameIssuer ? previous.SolPassword : null);
                config.CertificatePassword = valueOrExisting(request.CertificatePassword, sameIssuer ? previous.CertificatePassword : null);
                config.CertificatePath = sameIssuer ? previous.CertificatePath : null;
                if (request.GuideEnabled) {
                    config.GuideClientId = request.GuideClientId == null ? "" : request.GuideClientId.trim();
                    config.GuideClientSecret = valueOrExisting(request.GuideClientSecret,
                            sameIssuer && config.GuideClientId.equals(previous.GuideClientId) ? previous.GuideClientSecret : null);
                    if (config.GuideClientId.isBlank() || config.GuideClientSecret == null || config.GuideClientSecret.isBlank()) {
                        throw new IllegalArgumentException("Complete Client ID y Client Secret para enviar guías de remisión");
                    }
                }
                if (certificate != null && !certificate.isEmpty()) {
                    uploadedPath = sunatCertificateService.storeProduction(certificate, config.CertificatePassword);
                    config.CertificatePath = uploadedPath;
                }
            }
            config.validate();
            sunatCertificateService.load(config);
            saveConfiguration(config, userCod);
            return sunatConfigSearchService.findInitializationForm();
        } catch (RuntimeException exception) {
            if (uploadedPath != null) sunatCertificateService.delete(uploadedPath);
            throw exception;
        }
    }

    private String valueOrExisting(String value, String existing) {
        return value == null || value.isBlank() ? existing : value;
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
