package com.ccadmin.app.sunat.service;

import com.ccadmin.app.shared.model.entity.BusinessConfigEntity;
import com.ccadmin.app.shared.service.BusinessConfigCreateService;
import com.ccadmin.app.shared.service.BusinessConfigSearchService;
import com.ccadmin.app.shared.service.SessionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;

@Service
public class SunatInitializationCreateService extends SessionService {
    private static final Set<String> DOCUMENT_URLS = Set.of("01_invoice", "03_receipt", "07_creditNote", "08_debitNote");
    private final SunatInitializationSearchService sunatInitializationSearchService;
    private final SunatConfigurationClientService sunatConfigurationClientService;
    private final BusinessConfigSearchService businessConfigSearchService;
    private final BusinessConfigCreateService businessConfigCreateService;
    private final ObjectMapper objectMapper;

    public SunatInitializationCreateService(SunatInitializationSearchService sunatInitializationSearchService,
            SunatConfigurationClientService sunatConfigurationClientService,
            BusinessConfigSearchService businessConfigSearchService,
            BusinessConfigCreateService businessConfigCreateService, ObjectMapper objectMapper) {
        this.sunatInitializationSearchService = sunatInitializationSearchService;
        this.sunatConfigurationClientService = sunatConfigurationClientService;
        this.businessConfigSearchService = businessConfigSearchService;
        this.businessConfigCreateService = businessConfigCreateService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ObjectNode configure(String configuration, MultipartFile certificate) throws java.io.IOException {
        sunatInitializationSearchService.requireRoot();
        var parsed = objectMapper.readTree(configuration);
        if (!(parsed instanceof ObjectNode request) || !Set.of("NONE", "BETA", "PRODUCCION")
                .contains(parsed.path("Mode").asText())) {
            throw new IllegalArgumentException("Seleccione cómo desea usar SUNAT");
        }
        String mode = request.path("Mode").asText();
        request.put("AuditUserCod", getUserCod());
        request.put("IssuerRuc", sunatInitializationSearchService.issuerRuc());
        List<BusinessConfigEntity> urls = businessConfigSearchService.findByGroupCod("UrlServiciosSunat");
        if (!"NONE".equals(mode) && !urls.stream().map(url -> url.ConfigCod).toList().containsAll(DOCUMENT_URLS)) {
            throw new IllegalArgumentException("Faltan las direcciones de comprobantes del servicio SUNAT");
        }
        if ("PRODUCCION".equals(mode) && request.path("GuideEnabled").asBoolean()
                && urls.stream().noneMatch(url -> "09_despatchAdvice".equals(url.ConfigCod))) {
            throw new IllegalArgumentException("Falta la dirección de guías del servicio SUNAT");
        }
        // Deshabilitar primero: si el servicio remoto falla, la transacción local se revierte.
        for (BusinessConfigEntity url : urls) {
            if (DOCUMENT_URLS.contains(url.ConfigCod) || "09_despatchAdvice".equals(url.ConfigCod)) {
                businessConfigCreateService.disable(url);
            }
        }
        ObjectNode result = sunatConfigurationClientService.configure(request, certificate);
        if (!"NONE".equals(mode)) {
            boolean guideEnabled = "PRODUCCION".equals(mode) && result.path("GuideEnabled").asBoolean();
            for (BusinessConfigEntity url : urls) {
                if (DOCUMENT_URLS.contains(url.ConfigCod) || (guideEnabled && "09_despatchAdvice".equals(url.ConfigCod))) {
                    businessConfigCreateService.enable(url);
                }
            }
        }
        return result;
    }
}
