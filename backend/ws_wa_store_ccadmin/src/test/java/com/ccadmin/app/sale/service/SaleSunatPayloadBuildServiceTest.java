package com.ccadmin.app.sale.service;

import com.ccadmin.app.client.model.entity.ClientEntity;
import com.ccadmin.app.person.model.entity.PersonEntity;
import com.ccadmin.app.sale.model.constants.SaleConstants;
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

        assertEquals("La factura requiere un cliente juridico con RUC valido", exception.getMessage());
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
        assertEquals("La boleta mayor a S/ 700 requiere un cliente identificado", exception.getMessage());
    }

    private ClientEntity rucClient() {
        PersonEntity person = new PersonEntity();
        person.PersonType = "04";
        person.DocumentType = "06";
        person.DocumentNum = "20123456789";
        person.BusinessName = "CLIENTE EMPRESA SAC";
        ClientEntity client = new ClientEntity();
        client.ClientCod = "CL001";
        client.Person = person;
        return client;
    }
}
