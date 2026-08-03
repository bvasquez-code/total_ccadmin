package com.ccadmin.app.sale.service;

import com.ccadmin.app.sale.model.dto.SaleDetailDto;
import com.ccadmin.app.sale.model.entity.SaleDocumentEntity;
import com.ccadmin.app.sale.repository.SaleDocumentRepository;
import com.ccadmin.app.sunat.model.dto.sunat.SunatInvoiceProcessRequestDto;
import com.ccadmin.app.sunat.model.dto.sunat.SunatReceiptProcessRequestDto;
import com.ccadmin.app.sunat.service.SaleSunatClientService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SaleSunatEmissionService {

    @Autowired
    private SaleSearchService saleSearchService;

    @Autowired
    private SaleSunatPayloadBuildService saleSunatPayloadBuildService;

    @Autowired
    private SaleSunatClientService saleSunatClientService;

    @Autowired
    private SaleDocumentRepository saleDocumentRepository;

    public void emitSale(String saleCod) {
        SaleDocumentEntity document = this.saleDocumentRepository.findFiscalBySaleCod(saleCod);
        if (document == null) {
            log.info("VENTA SIN DOCUMENTO FISCAL PARA SUNAT : {}", saleCod);
            return;
        }
        this.emitSale(saleCod, document.DocumentCod);
    }

    public void emitSale(String saleCod, String documentCod) {
        log.info("INI - EMISION SUNAT VENTA : {} DOCUMENTO : {}", saleCod, documentCod);
        SaleDetailDto saleDetail = this.saleSearchService.findById(saleCod);
        SaleDocumentEntity selectedDocument = saleDetail.SaleDocumentList == null
                ? null
                : saleDetail.SaleDocumentList.stream()
                .filter(document -> documentCod != null && documentCod.equals(document.DocumentCod))
                .findFirst()
                .orElse(null);
        if (selectedDocument == null) {
            throw new IllegalArgumentException(
                    "El documento " + documentCod + " no pertenece a la venta " + saleCod
            );
        }
        saleDetail.SaleDocument = selectedDocument;
        saleDetail.Headboard.Client = selectedDocument.Client;
        if (!this.saleSunatPayloadBuildService.isInvoiceOrReceipt(saleDetail)) {
            log.info("DOCUMENTO NO FISCAL OMITIDO PARA SUNAT : {} - {}", saleCod, documentCod);
            return;
        }
        Object response;
        if (this.saleSunatPayloadBuildService.isInvoice(saleDetail)) {
            SunatInvoiceProcessRequestDto request = this.saleSunatPayloadBuildService.buildInvoice(saleDetail);
            response = this.saleSunatClientService.processInvoice(request).Data;
        } else if (this.saleSunatPayloadBuildService.isReceipt(saleDetail)) {
            SunatReceiptProcessRequestDto request = this.saleSunatPayloadBuildService.buildReceipt(saleDetail);
            response = this.saleSunatClientService.processReceipt(request).Data;
        } else {
            throw new IllegalArgumentException("Documento de venta no corresponde a factura o boleta SUNAT");
        }
        log.info("FIN - EMISION SUNAT VENTA : {} DOCUMENTO : {} -> {}", saleCod, documentCod, response);
    }
}
