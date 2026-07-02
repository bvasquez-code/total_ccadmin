package com.ccadmin.app.sale.service;

import com.ccadmin.app.sale.model.dto.SaleDetailDto;
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

    public void emitSale(String saleCod) {
        log.info("INI - EMISION SUNAT VENTA : {}", saleCod);
        SaleDetailDto saleDetail = this.saleSearchService.findById(saleCod);
        if (!this.saleSunatPayloadBuildService.isInvoiceOrReceipt(saleDetail)) {
            log.info("VENTA SIN DOCUMENTO FACTURA/BOLETA PARA SUNAT : {}", saleCod);
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
        log.info("FIN - EMISION SUNAT VENTA : {} -> {}", saleCod, response);
    }
}
