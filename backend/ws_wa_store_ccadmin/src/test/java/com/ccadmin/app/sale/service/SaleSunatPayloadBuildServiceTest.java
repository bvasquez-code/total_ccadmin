package com.ccadmin.app.sale.service;

import com.ccadmin.app.sale.model.constants.SaleConstants;
import com.ccadmin.app.sale.model.entity.SaleBillingEntity;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SaleSunatPayloadBuildServiceTest {

    private final SaleSunatPayloadBuildService service = new SaleSunatPayloadBuildService();

    @Test
    void acceptsLegalClientWithRucForInvoice() {
        assertDoesNotThrow(() -> service.validateCustomerForDocument(
                rucClient(),
                SaleConstants.DOCUMENT_TYPE_INVOICE,
                new BigDecimal("1000.00")
        ));
    }

    @Test
    void rejectsInvoiceWithoutRucClient() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.validateCustomerForDocument(
                        null,
                        SaleConstants.DOCUMENT_TYPE_INVOICE,
                        new BigDecimal("100.00")
                )
        );

        assertEquals("La factura requiere una persona con RUC valido y razon social", exception.getMessage());
    }

    @Test
    void onlyAllowsAnonymousReceiptUpToSevenHundred() {
        assertDoesNotThrow(() -> service.validateCustomerForDocument(
                null,
                SaleConstants.DOCUMENT_TYPE_RECEIPT,
                new BigDecimal("700.00")
        ));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.validateCustomerForDocument(
                        null,
                        SaleConstants.DOCUMENT_TYPE_RECEIPT,
                        new BigDecimal("700.01")
                )
        );
        assertEquals("La boleta mayor a S/ 700 requiere una persona identificada", exception.getMessage());
    }

    private SaleBillingEntity rucClient() {
        SaleBillingEntity billing = new SaleBillingEntity();
        billing.DocumentType = "06";
        billing.DocumentNum = "20123456789";
        billing.LegalName = "CLIENTE EMPRESA SAC";
        return billing;
    }
}
