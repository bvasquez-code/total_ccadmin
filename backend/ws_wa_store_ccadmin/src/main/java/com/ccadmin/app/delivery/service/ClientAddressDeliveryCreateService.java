package com.ccadmin.app.delivery.service;

import com.ccadmin.app.sale.model.entity.ClientAddressEntity;
import com.ccadmin.app.sale.repository.ClientAddressRepository;
import com.ccadmin.app.shared.model.constants.AuditUserConstants;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class ClientAddressDeliveryCreateService {

    private final ClientDeliveryContextService clientDeliveryContextService;
    private final ClientAddressRepository clientAddressRepository;

    public ClientAddressDeliveryCreateService(
            ClientDeliveryContextService clientDeliveryContextService,
            ClientAddressRepository clientAddressRepository
    ) {
        this.clientDeliveryContextService = clientDeliveryContextService;
        this.clientAddressRepository = clientAddressRepository;
    }

    @Transactional
    public ClientAddressEntity save(ClientAddressEntity request) {
        if (request == null) {
            throw new IllegalArgumentException("Los datos de la dirección son obligatorios");
        }
        validate(request);

        String clientCod = clientDeliveryContextService.getCurrentClient().ClientCod;
        boolean isFirstAddress = clientAddressRepository.findActiveByClientCod(clientCod).isEmpty();
        boolean makeDefault = isFirstAddress || "S".equalsIgnoreCase(request.IsDefault);

        if (makeDefault) {
            clientAddressRepository.clearDefaultByClientCod(
                    clientCod,
                    AuditUserConstants.USER_WEB
            );
        }

        request.ClientAddressID = null;
        request.ClientCod = clientCod;
        request.Alias = normalizeOptional(request.Alias);
        request.Names = request.Names.trim();
        request.Phone = request.Phone.trim();
        request.Address = request.Address.trim();
        request.Reference = normalizeOptional(request.Reference);
        request.UbigeoCod = normalizeOptional(request.UbigeoCod);
        request.Instructions = normalizeOptional(request.Instructions);
        request.IsDefault = makeDefault ? "S" : "N";
        request.Status = "A";
        request.CreationUser = null;
        request.CreationDate = null;
        request.ModifyUser = null;
        request.ModifyDate = null;
        request.addSession(AuditUserConstants.USER_WEB);
        request.ModifyDate = request.CreationDate;
        return clientAddressRepository.save(request);
    }

    private void validate(ClientAddressEntity request) {
        validateRequired(request.Names, 256, "El nombre de contacto");
        validateRequired(request.Phone, 20, "El teléfono");
        validateRequired(request.Address, 256, "La dirección");
        validateOptional(request.Alias, 64, "El alias");
        validateOptional(request.Reference, 256, "La referencia");
        validateOptional(request.UbigeoCod, 12, "El ubigeo");
        validateOptional(request.Instructions, 256, "Las indicaciones");

        if (request.Latitude == null || request.Longitude == null
                || request.Latitude.compareTo(BigDecimal.valueOf(-90)) < 0
                || request.Latitude.compareTo(BigDecimal.valueOf(90)) > 0
                || request.Longitude.compareTo(BigDecimal.valueOf(-180)) < 0
                || request.Longitude.compareTo(BigDecimal.valueOf(180)) > 0) {
            throw new IllegalArgumentException("Selecciona una ubicación válida en el mapa");
        }
        if (!"S".equalsIgnoreCase(request.IsDefault)
                && !"N".equalsIgnoreCase(request.IsDefault)) {
            throw new IllegalArgumentException("El indicador de dirección predeterminada debe ser S o N");
        }
    }

    private void validateRequired(String value, int maximumLength, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " es obligatorio");
        }
        validateOptional(value, maximumLength, fieldName);
    }

    private void validateOptional(String value, int maximumLength, String fieldName) {
        if (value != null && value.trim().length() > maximumLength) {
            throw new IllegalArgumentException(fieldName + " supera el tamaño permitido");
        }
    }

    private String normalizeOptional(String value) {
        return value == null ? "" : value.trim();
    }
}
