package com.ccadmin.app.sale.service;

import com.ccadmin.app.sale.model.dto.SaleDetailDto;
import com.ccadmin.app.sale.model.dto.sunat.SunatElectronicDocumentDto;
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
        SunatElectronicDocumentDto request = this.saleSunatPayloadBuildService.build(saleDetail);
        Object response = this.saleSunatClientService.process(request).Data;
        log.info("FIN - EMISION SUNAT VENTA : {} -> {}", saleCod, response);
    }
}
