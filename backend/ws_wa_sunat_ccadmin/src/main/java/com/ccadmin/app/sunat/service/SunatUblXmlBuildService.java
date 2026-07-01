package com.ccadmin.app.sunat.service;

import com.ccadmin.app.sunat.model.constants.SunatDocumentTypeConst;
import com.ccadmin.app.sunat.model.dto.SunatElectronicDocumentDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SunatUblXmlBuildService {

    @Autowired
    private SunatFacturaXmlBuildService sunatFacturaXmlBuildService;

    @Autowired
    private SunatBoletaXmlBuildService sunatBoletaXmlBuildService;

    @Autowired
    private SunatNotaCreditoXmlBuildService sunatNotaCreditoXmlBuildService;

    @Autowired
    private SunatNotaDebitoXmlBuildService sunatNotaDebitoXmlBuildService;

    @Autowired
    private SunatGuiaRemisionXmlBuildService sunatGuiaRemisionXmlBuildService;

    public String build(SunatElectronicDocumentDto dto) {
        return switch (dto.SunatDocumentType) {
            case SunatDocumentTypeConst.FACTURA -> buildXmlFactura(dto);
            case SunatDocumentTypeConst.BOLETA -> buildXmlBoleta(dto);
            case SunatDocumentTypeConst.NOTA_CREDITO -> buildXmlNotaCredito(dto);
            case SunatDocumentTypeConst.NOTA_DEBITO -> buildXmlNotaDebito(dto);
            case SunatDocumentTypeConst.GUIA_REMISION_REMITENTE -> buildXmlGuiaRemision(dto);
            default -> throw new IllegalArgumentException("Tipo de documento SUNAT no soportado para XML: " + dto.SunatDocumentType);
        };
    }

    public String buildXmlFactura(SunatElectronicDocumentDto dto) {
        return this.sunatFacturaXmlBuildService.buildXmlFactura(dto);
    }

    public String buildXmlBoleta(SunatElectronicDocumentDto dto) {
        return this.sunatBoletaXmlBuildService.buildXmlBoleta(dto);
    }

    public String buildXmlNotaCredito(SunatElectronicDocumentDto dto) {
        return this.sunatNotaCreditoXmlBuildService.buildXmlNotaCredito(dto);
    }

    public String buildXmlNotaDebito(SunatElectronicDocumentDto dto) {
        return this.sunatNotaDebitoXmlBuildService.buildXmlNotaDebito(dto);
    }

    public String buildXmlGuiaRemision(SunatElectronicDocumentDto dto) {
        return this.sunatGuiaRemisionXmlBuildService.buildXmlGuiaRemision(dto);
    }
}
