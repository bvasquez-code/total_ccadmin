package com.ccadmin.app.delivery.service;

import com.ccadmin.app.client.model.entity.ClientAccountEntity;
import com.ccadmin.app.client.model.entity.ClientEntity;
import com.ccadmin.app.client.repository.ClientAccountRepository;
import com.ccadmin.app.client.repository.ClientRepository;
import com.ccadmin.app.delivery.model.dto.ClientProfileDto;
import com.ccadmin.app.delivery.model.dto.ClientProfileUpdateDto;
import com.ccadmin.app.person.model.entity.PersonEntity;
import com.ccadmin.app.person.repository.PersonRepository;
import com.ccadmin.app.shared.model.constants.AuditUserConstants;
import com.ccadmin.app.shared.model.dto.ClientSessionDto;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
public class ClientProfileDeliveryService {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9]{7,20}$");

    private final ClientDeliveryContextService clientDeliveryContextService;
    private final ClientAccountRepository clientAccountRepository;
    private final ClientRepository clientRepository;
    private final PersonRepository personRepository;

    public ClientProfileDeliveryService(
            ClientDeliveryContextService clientDeliveryContextService,
            ClientAccountRepository clientAccountRepository,
            ClientRepository clientRepository,
            PersonRepository personRepository
    ) {
        this.clientDeliveryContextService = clientDeliveryContextService;
        this.clientAccountRepository = clientAccountRepository;
        this.clientRepository = clientRepository;
        this.personRepository = personRepository;
    }

    public ClientProfileDto find() {
        ClientSessionDto clientSession = clientDeliveryContextService.getCurrentClient();
        ClientProfileDto profile = clientAccountRepository
                .findProfileByClientAccountID(clientSession.ClientAccountID)
                .map(ClientProfileDto::from)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró el perfil del cliente"));
        if (!clientSession.ClientCod.equals(profile.ClientCod)) {
            throw new IllegalArgumentException("El perfil solicitado no pertenece al cliente autenticado");
        }
        return profile;
    }

    @Transactional
    public ClientProfileDto update(ClientProfileUpdateDto request) {
        validate(request);
        ClientSessionDto clientSession = clientDeliveryContextService.getCurrentClient();
        ClientAccountEntity account = clientAccountRepository
                .findActiveByClientAccountID(clientSession.ClientAccountID)
                .orElseThrow(() -> new IllegalArgumentException("La cuenta del cliente ya no está disponible"));
        if (!clientSession.ClientCod.equals(account.ClientCod)) {
            throw new IllegalArgumentException("La cuenta no pertenece al cliente autenticado");
        }

        ClientEntity client = clientRepository.findActiveByClientCod(account.ClientCod)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró al cliente de la cuenta"));
        PersonEntity person = personRepository.findActiveByPersonCod(client.PersonCod)
                .orElseThrow(() -> new IllegalArgumentException("No se encontraron los datos personales del cliente"));

        person.Names = request.Names.trim();
        person.LastNames = request.LastNames.trim();
        person.CellPhone = request.Phone.trim();
        person.addSessionModify(AuditUserConstants.USER_WEB);
        personRepository.save(person);
        return find();
    }

    private void validate(ClientProfileUpdateDto request) {
        if (request == null) {
            throw new IllegalArgumentException("Los datos del perfil son obligatorios");
        }
        validateRequired(request.Names, 128, "Los nombres");
        validateRequired(request.LastNames, 128, "Los apellidos");
        if (request.Phone == null || !PHONE_PATTERN.matcher(request.Phone.trim()).matches()) {
            throw new IllegalArgumentException("El teléfono debe contener entre 7 y 20 números");
        }
    }

    private void validateRequired(String value, int maximumLength, String fieldName) {
        if (value == null || value.isBlank() || value.trim().length() > maximumLength) {
            throw new IllegalArgumentException(
                    fieldName + " son obligatorios y admiten hasta " + maximumLength + " caracteres"
            );
        }
    }
}
