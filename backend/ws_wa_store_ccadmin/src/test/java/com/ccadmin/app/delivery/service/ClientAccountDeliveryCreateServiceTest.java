package com.ccadmin.app.delivery.service;

import com.ccadmin.app.client.model.entity.ClientAccountEntity;
import com.ccadmin.app.client.model.entity.ClientEntity;
import com.ccadmin.app.client.repository.ClientAccountRepository;
import com.ccadmin.app.client.repository.ClientRepository;
import com.ccadmin.app.client.service.ClientService;
import com.ccadmin.app.delivery.model.dto.ClientLoginResponseDto;
import com.ccadmin.app.delivery.model.dto.ClientRegisterDto;
import com.ccadmin.app.person.model.entity.PersonEntity;
import com.ccadmin.app.person.repository.PersonRepository;
import com.ccadmin.app.shared.model.constants.AuditUserConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientAccountDeliveryCreateServiceTest {

    @Mock private ClientAccountRepository clientAccountRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private PersonRepository personRepository;
    @Mock private ClientRepository clientRepository;
    @Mock private ClientService clientService;

    private ClientAccountDeliveryCreateService service;

    @BeforeEach
    void setUp() {
        service = new ClientAccountDeliveryCreateService(
                clientAccountRepository,
                passwordEncoder,
                personRepository,
                clientRepository,
                clientService
        );
    }

    @Test
    void createsVerifiedAccountWithEncryptedPasswordAndStartsSession() {
        ClientRegisterDto request = registerRequest();
        when(clientAccountRepository.countByEmail("cliente@example.com")).thenReturn(0);
        when(personRepository.findByDocumentNum("01", "74285109")).thenReturn(null);
        when(clientRepository.findByPersonCod(any())).thenReturn(null);
        when(passwordEncoder.encode("segura123")).thenReturn("encrypted-password");
        when(clientService.saveWeb(any(), any())).thenAnswer(invocation -> {
            ClientEntity client = invocation.getArgument(0);
            client.ClientCod = client.Person.DocumentNum;
            client.PersonCod = client.Person.DocumentNum;
            client.Person.PersonCod = client.Person.DocumentNum;
            return client;
        });
        when(clientAccountRepository.save(any())).thenAnswer(invocation -> {
            ClientAccountEntity account = invocation.getArgument(0);
            account.ClientAccountID = 10L;
            return account;
        });

        ClientLoginResponseDto response = service.register(request);

        ArgumentCaptor<ClientEntity> clientCaptor = ArgumentCaptor.forClass(ClientEntity.class);
        verify(clientService).saveWeb(clientCaptor.capture(), org.mockito.ArgumentMatchers.eq(AuditUserConstants.USER_WEB));
        assertEquals("74285109", clientCaptor.getValue().Person.DocumentNum);
        assertEquals("cliente@example.com", clientCaptor.getValue().Person.Email);

        ArgumentCaptor<ClientAccountEntity> accountCaptor = ArgumentCaptor.forClass(ClientAccountEntity.class);
        verify(clientAccountRepository).save(accountCaptor.capture());
        assertEquals("74285109", accountCaptor.getValue().ClientCod);
        assertEquals("encrypted-password", accountCaptor.getValue().PasswordHash);
        assertEquals("S", accountCaptor.getValue().IsEmailVerified);
        assertEquals(AuditUserConstants.USER_WEB, accountCaptor.getValue().CreationUser);
        assertEquals("74285109", response.ClientCod);
        assertEquals("cliente@example.com", response.Email);
        assertNotNull(response.Token);
    }

    @Test
    void rejectsEmailThatAlreadyHasAnAccount() {
        ClientRegisterDto request = registerRequest();
        when(clientAccountRepository.countByEmail("cliente@example.com")).thenReturn(1);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.register(request)
        );

        assertEquals("El correo ya tiene una cuenta registrada", exception.getMessage());
        verify(clientService, never()).saveWeb(any(), any());
    }

    @Test
    void rejectsDocumentThatAlreadyHasAWebAccount() {
        ClientRegisterDto request = registerRequest();
        PersonEntity person = new PersonEntity();
        person.PersonCod = "74285109";
        when(clientAccountRepository.countByEmail("cliente@example.com")).thenReturn(0);
        when(personRepository.findByDocumentNum("01", "74285109")).thenReturn(person);
        when(clientAccountRepository.countByClientCod("74285109")).thenReturn(1);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.register(request)
        );

        assertEquals("El cliente ya tiene una cuenta web registrada", exception.getMessage());
        verify(clientService, never()).saveWeb(any(), any());
    }

    private ClientRegisterDto registerRequest() {
        ClientRegisterDto request = new ClientRegisterDto();
        request.DocumentType = "01";
        request.DocumentNumber = "74285109";
        request.Names = "Braulio Walter";
        request.LastNames = "Vásquez Vásquez";
        request.Phone = "999999999";
        request.Email = "CLIENTE@EXAMPLE.COM";
        request.Password = "segura123";
        request.ConfirmPassword = "segura123";
        return request;
    }
}
