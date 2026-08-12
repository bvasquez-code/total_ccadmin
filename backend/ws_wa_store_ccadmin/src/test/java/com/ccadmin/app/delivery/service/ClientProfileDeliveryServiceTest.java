package com.ccadmin.app.delivery.service;

import com.ccadmin.app.client.model.entity.ClientAccountEntity;
import com.ccadmin.app.client.model.entity.ClientEntity;
import com.ccadmin.app.client.repository.ClientAccountRepository;
import com.ccadmin.app.client.repository.ClientRepository;
import com.ccadmin.app.delivery.model.dto.ClientProfileDto;
import com.ccadmin.app.delivery.model.dto.ClientProfileUpdateDto;
import com.ccadmin.app.delivery.model.idto.IClientProfileDto;
import com.ccadmin.app.person.model.entity.PersonEntity;
import com.ccadmin.app.person.repository.PersonRepository;
import com.ccadmin.app.shared.model.constants.AuditUserConstants;
import com.ccadmin.app.shared.model.dto.ClientSessionDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientProfileDeliveryServiceTest {

    @Mock private ClientDeliveryContextService clientDeliveryContextService;
    @Mock private ClientAccountRepository clientAccountRepository;
    @Mock private ClientRepository clientRepository;
    @Mock private PersonRepository personRepository;

    private ClientProfileDeliveryService service;

    @BeforeEach
    void setUp() {
        service = new ClientProfileDeliveryService(
                clientDeliveryContextService,
                clientAccountRepository,
                clientRepository,
                personRepository
        );
    }

    @Test
    void updatesEditableFieldsAndKeepsDocumentAndEmailOutsideUpdateContract() {
        ClientSessionDto session = new ClientSessionDto(10L, "CL001", "client@example.com", "Cliente");
        ClientAccountEntity account = new ClientAccountEntity();
        account.ClientAccountID = 10L;
        account.ClientCod = "CL001";
        account.Email = "client@example.com";
        ClientEntity client = new ClientEntity();
        client.ClientCod = "CL001";
        client.PersonCod = "PER001";
        PersonEntity person = new PersonEntity();
        person.PersonCod = "PER001";
        person.DocumentType = "01";
        person.DocumentNum = "74285109";
        person.Email = "client@example.com";
        person.CreationUser = AuditUserConstants.USER_WEB;

        ClientProfileUpdateDto request = new ClientProfileUpdateDto();
        request.Names = "Juan Pedro";
        request.LastNames = "Pérez López";
        request.Phone = "999888777";

        IClientProfileDto projection = profileProjection();
        when(clientDeliveryContextService.getCurrentClient()).thenReturn(session);
        when(clientAccountRepository.findActiveByClientAccountID(10L)).thenReturn(Optional.of(account));
        when(clientRepository.findActiveByClientCod("CL001")).thenReturn(Optional.of(client));
        when(personRepository.findActiveByPersonCod("PER001")).thenReturn(Optional.of(person));
        when(clientAccountRepository.findProfileByClientAccountID(10L)).thenReturn(Optional.of(projection));

        ClientProfileDto result = service.update(request);

        assertEquals("Juan Pedro", person.Names);
        assertEquals("Pérez López", person.LastNames);
        assertEquals("999888777", person.CellPhone);
        assertEquals("74285109", person.DocumentNum);
        assertEquals("client@example.com", person.Email);
        assertEquals(AuditUserConstants.USER_WEB, person.ModifyUser);
        assertEquals("client@example.com", result.Email);
        verify(personRepository).save(person);
    }

    @Test
    void rejectsInvalidPhoneBeforeUpdatingPerson() {
        ClientProfileUpdateDto request = new ClientProfileUpdateDto();
        request.Names = "Juan";
        request.LastNames = "Pérez";
        request.Phone = "ABC";

        assertThrows(IllegalArgumentException.class, () -> service.update(request));

        verify(personRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    private IClientProfileDto profileProjection() {
        IClientProfileDto projection = mock(IClientProfileDto.class);
        when(projection.getClientAccountID()).thenReturn(10L);
        when(projection.getClientCod()).thenReturn("CL001");
        when(projection.getEmail()).thenReturn("client@example.com");
        when(projection.getDocumentType()).thenReturn("01");
        when(projection.getDocumentNumber()).thenReturn("74285109");
        when(projection.getNames()).thenReturn("Juan Pedro");
        when(projection.getLastNames()).thenReturn("Pérez López");
        when(projection.getPhone()).thenReturn("999888777");
        return projection;
    }
}
