package com.ccadmin.app.delivery.service;

import com.ccadmin.app.client.model.entity.ClientAccountEntity;
import com.ccadmin.app.client.model.entity.ClientEntity;
import com.ccadmin.app.client.repository.ClientAccountRepository;
import com.ccadmin.app.client.repository.ClientRepository;
import com.ccadmin.app.client.service.ClientService;
import com.ccadmin.app.delivery.model.dto.ClientLoginDto;
import com.ccadmin.app.delivery.model.dto.ClientLoginResponseDto;
import com.ccadmin.app.delivery.model.dto.ClientRegisterDto;
import com.ccadmin.app.person.model.entity.PersonEntity;
import com.ccadmin.app.person.repository.PersonRepository;
import com.ccadmin.app.security.util.TokenUtil;
import com.ccadmin.app.shared.model.constants.AuditUserConstants;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class ClientAccountDeliveryCreateService {

    private static final int MAX_FAILED_LOGIN_ATTEMPTS = 5;
    private static final long LOCK_TIME_MILLISECONDS = 15L * 60L * 1000L;
    private static final Set<String> ALLOWED_DOCUMENT_TYPES = Set.of("01", "04");
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9]{7,20}$");

    private final ClientAccountRepository clientAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final PersonRepository personRepository;
    private final ClientRepository clientRepository;
    private final ClientService clientService;

    public ClientAccountDeliveryCreateService(
            ClientAccountRepository clientAccountRepository,
            PasswordEncoder passwordEncoder,
            PersonRepository personRepository,
            ClientRepository clientRepository,
            ClientService clientService
    ) {
        this.clientAccountRepository = clientAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.personRepository = personRepository;
        this.clientRepository = clientRepository;
        this.clientService = clientService;
    }

    @Transactional(dontRollbackOn = IllegalArgumentException.class)
    public ClientLoginResponseDto login(ClientLoginDto request) {
        validateRequest(request);

        ClientAccountEntity clientAccount = clientAccountRepository
                .findActiveByEmail(request.Email.trim())
                .orElseThrow(() -> new IllegalArgumentException("Correo o contraseña incorrectos"));

        Date now = new Date();
        if (clientAccount.LockUntilDate != null && clientAccount.LockUntilDate.after(now)) {
            throw new IllegalArgumentException("La cuenta está bloqueada temporalmente. Intente nuevamente más tarde");
        }
        if (clientAccount.PasswordHash == null
                || !passwordEncoder.matches(request.Password, clientAccount.PasswordHash)) {
            registerFailedAttempt(clientAccount, now);
            throw new IllegalArgumentException("Correo o contraseña incorrectos");
        }
        if (!"S".equals(clientAccount.IsEmailVerified)) {
            throw new IllegalArgumentException("Debe verificar su correo antes de ingresar");
        }

        clientAccount.FailedLoginAttempts = 0;
        clientAccount.LockUntilDate = null;
        clientAccount.LastLoginDate = now;
        clientAccount.addSession(AuditUserConstants.USER_WEB);
        clientAccountRepository.save(clientAccount);

        return createSessionResponse(
                clientAccount,
                clientAccountRepository.findClientNames(clientAccount.ClientAccountID)
        );
    }

    @Transactional
    public ClientLoginResponseDto register(ClientRegisterDto request) {
        if (request == null) {
            throw new IllegalArgumentException("Los datos del registro son obligatorios");
        }
        normalizeRegisterRequest(request);
        validateRegisterRequest(request);

        if (clientAccountRepository.countByEmail(request.Email) > 0) {
            throw new IllegalArgumentException("El correo ya tiene una cuenta registrada");
        }

        PersonEntity person = personRepository.findByDocumentNum(
                request.DocumentType,
                request.DocumentNumber
        );
        if (person == null) {
            person = buildNewPerson(request);
        } else {
            if (clientAccountRepository.countByClientCod(person.PersonCod) > 0) {
                throw new IllegalArgumentException("El cliente ya tiene una cuenta web registrada");
            }
            person.Email = request.Email;
            person.CellPhone = request.Phone;
        }

        ClientEntity client = clientRepository.findByPersonCod(person.PersonCod);
        if (client == null) {
            client = new ClientEntity();
        }
        client.Person = person;
        client = clientService.saveWeb(client, AuditUserConstants.USER_WEB);

        ClientAccountEntity account = new ClientAccountEntity();
        account.ClientCod = client.ClientCod;
        account.Email = request.Email;
        account.PasswordHash = passwordEncoder.encode(request.Password);
        account.IsEmailVerified = "S";
        account.FailedLoginAttempts = 0;
        account.LastLoginDate = new Date();
        account.addSession(AuditUserConstants.USER_WEB);
        account = clientAccountRepository.save(account);

        return createSessionResponse(account, fullName(client.Person));
    }

    private void validateRequest(ClientLoginDto request) {
        if (request == null || request.Email == null || request.Email.isBlank()
                || request.Password == null || request.Password.isBlank()) {
            throw new IllegalArgumentException("El correo y la contraseña son obligatorios");
        }
    }

    private void registerFailedAttempt(ClientAccountEntity clientAccount, Date now) {
        clientAccount.FailedLoginAttempts++;
        if (clientAccount.FailedLoginAttempts >= MAX_FAILED_LOGIN_ATTEMPTS) {
            clientAccount.LockUntilDate = new Date(now.getTime() + LOCK_TIME_MILLISECONDS);
        }
        clientAccount.addSession(AuditUserConstants.USER_WEB);
        clientAccountRepository.save(clientAccount);
    }

    private void validateRegisterRequest(ClientRegisterDto request) {
        if (!ALLOWED_DOCUMENT_TYPES.contains(request.DocumentType)) {
            throw new IllegalArgumentException("El tipo de documento debe ser DNI o carnet de extranjería");
        }
        if (request.DocumentNumber == null || request.DocumentNumber.isBlank()
                || request.DocumentNumber.length() > 16) {
            throw new IllegalArgumentException("El número de documento es obligatorio y admite hasta 16 caracteres");
        }
        if ("01".equals(request.DocumentType)
                && !request.DocumentNumber.matches("^[0-9]{8}$")) {
            throw new IllegalArgumentException("El DNI debe contener exactamente 8 números");
        }
        if ("04".equals(request.DocumentType)
                && !request.DocumentNumber.matches("^[A-Za-z0-9]{9,16}$")) {
            throw new IllegalArgumentException("El carnet de extranjería debe contener entre 9 y 16 caracteres");
        }
        validateRequiredLength(request.Names, 128, "Los nombres");
        validateRequiredLength(request.LastNames, 128, "Los apellidos");
        if (request.Phone == null || !PHONE_PATTERN.matcher(request.Phone.trim()).matches()) {
            throw new IllegalArgumentException("El teléfono debe contener entre 7 y 20 números");
        }
        if (request.Email == null || request.Email.isBlank() || request.Email.length() > 32
                || !EMAIL_PATTERN.matcher(request.Email.trim()).matches()) {
            throw new IllegalArgumentException("Ingrese un correo válido de hasta 32 caracteres");
        }
        if (request.Password == null || request.Password.length() < 8 || request.Password.length() > 72) {
            throw new IllegalArgumentException("La contraseña debe contener entre 8 y 72 caracteres");
        }
        if (!request.Password.equals(request.ConfirmPassword)) {
            throw new IllegalArgumentException("Las contraseñas no coinciden");
        }
    }

    private void normalizeRegisterRequest(ClientRegisterDto request) {
        request.DocumentType = trim(request.DocumentType);
        request.DocumentNumber = trim(request.DocumentNumber).toUpperCase();
        request.Names = trim(request.Names);
        request.LastNames = trim(request.LastNames);
        request.Phone = trim(request.Phone);
        request.Email = trim(request.Email).toLowerCase();
    }

    private PersonEntity buildNewPerson(ClientRegisterDto request) {
        PersonEntity person = new PersonEntity();
        person.PersonType = "01";
        person.DocumentType = request.DocumentType;
        person.DocumentNum = request.DocumentNumber;
        person.Names = request.Names;
        person.LastNames = request.LastNames;
        person.Email = request.Email;
        person.CellPhone = request.Phone;
        return person;
    }

    private ClientLoginResponseDto createSessionResponse(
            ClientAccountEntity clientAccount,
            String names
    ) {
        String token = TokenUtil.createClientToken(clientAccount.ClientAccountID, clientAccount.Email);
        return new ClientLoginResponseDto(
                "Bearer " + token,
                clientAccount.ClientAccountID,
                clientAccount.ClientCod,
                clientAccount.Email,
                names
        );
    }

    private String fullName(PersonEntity person) {
        return (person.Names + " " + person.LastNames).trim();
    }

    private void validateRequiredLength(String value, int maximum, String fieldName) {
        if (value == null || value.isBlank() || value.trim().length() > maximum) {
            throw new IllegalArgumentException(fieldName + " son obligatorios y admiten hasta " + maximum + " caracteres");
        }
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
