package com.ccadmin.app.sale.service;

import com.ccadmin.app.client.model.entity.ClientEntity;
import com.ccadmin.app.client.shared.ClientShared;
import com.ccadmin.app.person.model.entity.PersonEntity;
import com.ccadmin.app.person.shared.PersonShared;
import com.ccadmin.app.sale.exception.SaleException;
import com.ccadmin.app.sale.model.constants.SaleConstants;
import com.ccadmin.app.sale.model.entity.SaleBillingEntity;
import com.ccadmin.app.sale.model.entity.SaleHeadEntity;
import com.ccadmin.app.sale.repository.SaleBillingRepository;
import com.ccadmin.app.sale.repository.SaleDocumentRepository;
import com.ccadmin.app.sale.repository.SaleHeadRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SaleBillingCreateServiceTest {

    @Mock
    private SaleBillingRepository saleBillingRepository;
    @Mock
    private SaleHeadRepository saleHeadRepository;
    @Mock
    private SaleDocumentRepository saleDocumentRepository;
    @Mock
    private ClientShared clientShared;
    @Mock
    private PersonShared personShared;
    @Mock
    private SaleSunatPayloadBuildService saleSunatPayloadBuildService;

    @Test
    void receiptAlwaysUsesTheBuyerIdentity() throws Exception {
        SaleBillingCreateService service = createService();
        SaleHeadEntity saleHead = sale("CLIENT01");
        PersonEntity buyerPerson = naturalPerson("PERSON01", "77889966", "Juan Perez");
        ClientEntity buyer = new ClientEntity();
        buyer.ClientCod = saleHead.ClientCod;
        buyer.PersonCod = buyerPerson.PersonCod;
        buyer.Person = buyerPerson;

        SaleBillingEntity request = new SaleBillingEntity();
        request.DocumentTypeRequest = SaleConstants.DOCUMENT_TYPE_RECEIPT;

        when(clientShared.findById(saleHead.ClientCod)).thenReturn(buyer);
        when(saleBillingRepository.save(any(SaleBillingEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SaleBillingEntity result = service.createForSale(saleHead, request, "SELLER");

        assertEquals(saleHead.SaleCod, result.SaleCod);
        assertEquals(SaleConstants.DOCUMENT_TYPE_RECEIPT, result.DocumentTypeRequest);
        assertEquals(buyerPerson.PersonCod, result.PersonCod);
        assertEquals(buyerPerson.DocumentNum, result.DocumentNum);
        assertEquals("Juan Perez", result.LegalName);
        verify(personShared, never()).saveWeb(any(), any());
    }

    @Test
    void invoiceCanUseAPersonDifferentFromTheBuyer() throws Exception {
        SaleBillingCreateService service = createService();
        SaleHeadEntity saleHead = sale("CLIENT01");
        PersonEntity selectedCompany = company("20601234567", "Empresa Facturada S.A.C.");

        SaleBillingEntity request = new SaleBillingEntity();
        request.DocumentTypeRequest = SaleConstants.DOCUMENT_TYPE_INVOICE;
        request.Person = selectedCompany;

        when(personShared.findByDocumentNum("06", selectedCompany.DocumentNum)).thenReturn(null);
        when(personShared.saveWeb(selectedCompany, "SELLER")).thenReturn(selectedCompany);
        when(saleBillingRepository.save(any(SaleBillingEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SaleBillingEntity result = service.createForSale(saleHead, request, "SELLER");

        assertEquals(SaleConstants.DOCUMENT_TYPE_INVOICE, result.DocumentTypeRequest);
        assertEquals(selectedCompany.PersonCod, result.PersonCod);
        assertEquals(selectedCompany.DocumentNum, result.DocumentNum);
        assertEquals(selectedCompany.BusinessName, result.LegalName);
        verify(clientShared, never()).findById(any());
    }

    @Test
    void receiptRejectsAPersonDifferentFromTheBuyer() {
        SaleBillingCreateService service = createService();
        SaleHeadEntity saleHead = sale("CLIENT01");
        PersonEntity buyerPerson = naturalPerson("PERSON01", "77889966", "Juan Perez");
        ClientEntity buyer = new ClientEntity();
        buyer.ClientCod = saleHead.ClientCod;
        buyer.PersonCod = buyerPerson.PersonCod;
        buyer.Person = buyerPerson;

        SaleBillingEntity request = new SaleBillingEntity();
        request.DocumentTypeRequest = SaleConstants.DOCUMENT_TYPE_RECEIPT;
        request.PersonCod = "PERSON02";

        when(clientShared.findById(saleHead.ClientCod)).thenReturn(buyer);

        SaleException exception = assertThrows(
                SaleException.class,
                () -> service.createForSale(saleHead, request, "SELLER")
        );

        assertEquals("La boleta debe emitirse a la misma persona asociada al comprador", exception.getMessage());
        verify(saleBillingRepository, never()).save(any());
    }

    @Test
    void saleWithoutRequestedDocumentCanKeepAnonymousBilling() throws Exception {
        SaleBillingCreateService service = createService();
        SaleHeadEntity saleHead = sale(null);
        when(saleBillingRepository.save(any(SaleBillingEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SaleBillingEntity result = service.createForSale(saleHead, null, "SELLER");

        assertNull(result.DocumentTypeRequest);
        assertNull(result.PersonCod);
        assertNull(result.DocumentNum);
    }

    private SaleBillingCreateService createService() {
        return new SaleBillingCreateService(
                saleBillingRepository,
                saleHeadRepository,
                saleDocumentRepository,
                clientShared,
                personShared,
                saleSunatPayloadBuildService
        );
    }

    private SaleHeadEntity sale(String clientCod) {
        SaleHeadEntity saleHead = new SaleHeadEntity();
        saleHead.SaleCod = "ST00100010000001";
        saleHead.ClientCod = clientCod;
        return saleHead;
    }

    private PersonEntity naturalPerson(String personCod, String documentNum, String names) {
        PersonEntity person = new PersonEntity();
        person.PersonCod = personCod;
        person.PersonType = "01";
        person.DocumentType = "01";
        person.DocumentNum = documentNum;
        person.Names = names;
        return person;
    }

    private PersonEntity company(String ruc, String businessName) {
        PersonEntity person = new PersonEntity();
        person.PersonCod = ruc;
        person.PersonType = "04";
        person.DocumentType = "06";
        person.DocumentNum = ruc;
        person.BusinessName = businessName;
        person.Address = "Av. Principal 123";
        person.UbigeoCod = "140101";
        return person;
    }
}
