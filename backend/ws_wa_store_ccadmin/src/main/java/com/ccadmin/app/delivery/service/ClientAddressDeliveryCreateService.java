package com.ccadmin.app.delivery.service;

import com.ccadmin.app.sale.model.entity.ClientAddressEntity;
import com.ccadmin.app.sale.repository.ClientAddressRepository;
import com.ccadmin.app.shared.model.constants.AuditUserConstants;
import com.ccadmin.app.shared.repository.UbigeoRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ClientAddressDeliveryCreateService {

    private final ClientDeliveryContextService clientDeliveryContextService;
    private final ClientAddressRepository clientAddressRepository;
    private final UbigeoRepository ubigeoRepository;

    public ClientAddressDeliveryCreateService(
            ClientDeliveryContextService clientDeliveryContextService,
            ClientAddressRepository clientAddressRepository,
            UbigeoRepository ubigeoRepository
    ) {
        this.clientDeliveryContextService = clientDeliveryContextService;
        this.clientAddressRepository = clientAddressRepository;
        this.ubigeoRepository = ubigeoRepository;
    }

    @Transactional
    public ClientAddressEntity save(ClientAddressEntity request) {
        if (request == null) {
            throw new IllegalArgumentException("Los datos de la dirección son obligatorios");
        }
        validate(request);

        String clientCod = clientDeliveryContextService.getCurrentClient().ClientCod;
        List<ClientAddressEntity> activeAddressList = clientAddressRepository.findActiveByClientCod(clientCod);
        boolean isNewAddress = request.ClientAddressID == null;
        ClientAddressEntity address = isNewAddress
                ? new ClientAddressEntity()
                : clientAddressRepository.findActiveByClientAddressIdAndClientCod(
                        request.ClientAddressID,
                        clientCod
                ).orElseThrow(() -> new IllegalArgumentException(
                        "La dirección que desea editar no pertenece al cliente o ya no está disponible"
                ));
        boolean makeDefault = activeAddressList.isEmpty()
                || "S".equalsIgnoreCase(request.IsDefault)
                || (!isNewAddress && "S".equals(address.IsDefault));

        if (makeDefault) {
            clientAddressRepository.clearDefaultByClientCod(
                    clientCod,
                    AuditUserConstants.USER_WEB
            );
        }

        address.ClientCod = clientCod;
        address.Alias = normalizeOptional(request.Alias);
        address.Names = request.Names.trim();
        address.Phone = request.Phone.trim();
        address.Address = request.Address.trim();
        address.Reference = normalizeOptional(request.Reference);
        address.UbigeoCod = request.UbigeoCod.trim();
        address.Latitude = request.Latitude;
        address.Longitude = request.Longitude;
        address.Instructions = normalizeOptional(request.Instructions);
        address.IsDefault = makeDefault ? "S" : "N";
        address.Status = "A";
        address.addSession(AuditUserConstants.USER_WEB, isNewAddress);
        if (isNewAddress) {
            address.ModifyDate = address.CreationDate;
        }
        return clientAddressRepository.save(address);
    }

    private void validate(ClientAddressEntity request) {
        validateRequired(request.Names, 256, "El nombre de contacto");
        validateRequired(request.Phone, 20, "El teléfono");
        validateRequired(request.Address, 256, "La dirección");
        validateOptional(request.Alias, 64, "El alias");
        validateOptional(request.Reference, 256, "La referencia");
        validateRequired(request.UbigeoCod, 6, "El ubigeo");
        validateOptional(request.Instructions, 256, "Las indicaciones");

        String ubigeoCod = request.UbigeoCod.trim();
        if (!ubigeoCod.matches("^\\d{6}$") || ubigeoRepository.countDistrictByCode(ubigeoCod) != 1) {
            throw new IllegalArgumentException("Selecciona un departamento, provincia y distrito validos");
        }

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
