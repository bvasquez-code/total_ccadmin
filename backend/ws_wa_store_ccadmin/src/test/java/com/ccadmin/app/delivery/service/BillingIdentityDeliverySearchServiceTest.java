package com.ccadmin.app.delivery.service;

import com.ccadmin.app.person.model.entity.PersonEntity;
import com.ccadmin.app.person.shared.PersonShared;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BillingIdentityDeliverySearchServiceTest {

    @Mock
    private PersonShared personShared;

    @Test
    void searchesAnInternalPersonUsingRucDocumentType() {
        PersonEntity expected = new PersonEntity();
        expected.DocumentNum = "20100017491";
        when(personShared.findByDocumentNum("06", expected.DocumentNum)).thenReturn(expected);
        BillingIdentityDeliverySearchService service = new BillingIdentityDeliverySearchService(personShared);

        PersonEntity result = service.findCompanyByRuc("20100017491");

        assertEquals(expected, result);
        verify(personShared).findByDocumentNum("06", "20100017491");
    }

    @Test
    void rejectsAnInvalidRucBeforeSearching() {
        BillingIdentityDeliverySearchService service = new BillingIdentityDeliverySearchService(personShared);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.findCompanyByRuc("123")
        );

        assertEquals("El RUC debe contener 11 digitos", exception.getMessage());
    }
}
