package com.ccadmin.app.delivery.service;

import com.ccadmin.app.sale.model.entity.ClientAddressEntity;
import com.ccadmin.app.sale.repository.ClientAddressRepository;
import com.ccadmin.app.shared.model.constants.AuditUserConstants;
import com.ccadmin.app.shared.model.idto.IAddressLocationDto;
import com.ccadmin.app.shared.repository.LocationRepository;
import com.ccadmin.app.shared.repository.UbigeoRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ClientAddressDeliveryCreateService {

    private static final String PERU_COUNTRY_COD = "PER";

    private final ClientDeliveryContextService clientDeliveryContextService;
    private final ClientAddressRepository clientAddressRepository;
    private final UbigeoRepository ubigeoRepository;
    private final LocationRepository locationRepository;

    public ClientAddressDeliveryCreateService(
            ClientDeliveryContextService clientDeliveryContextService,
            ClientAddressRepository clientAddressRepository,
            UbigeoRepository ubigeoRepository,
            LocationRepository locationRepository
    ) {
        this.clientDeliveryContextService = clientDeliveryContextService;
        this.clientAddressRepository = clientAddressRepository;
        this.ubigeoRepository = ubigeoRepository;
        this.locationRepository = locationRepository;
    }

    @Transactional
    public ClientAddressEntity save(ClientAddressEntity request) {
        if (request == null) {
            throw new IllegalArgumentException("Los datos de la dirección son obligatorios");
        }
        validate(request);
        IAddressLocationDto location = resolveLocation(request);

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
        address.CountryCod = location.getCountryCod();
        address.CountryName = location.getCountryName();
        address.StateName = location.getStateName();
        address.CityName = location.getCityName();
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
        validateRequired(request.CountryCod, 3, "El país");
        validateOptional(request.Alias, 64, "El alias");
        validateOptional(request.Reference, 256, "La referencia");
        validateRequired(request.UbigeoCod, 12, "El ubigeo");
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

    private IAddressLocationDto resolveLocation(ClientAddressEntity request) {
        String countryCod = request.CountryCod.trim().toUpperCase();
        String ubigeoCod = request.UbigeoCod.trim();

        if (PERU_COUNTRY_COD.equals(countryCod)) {
            if (!ubigeoCod.matches("^\\d{6}$")) {
                throw new IllegalArgumentException("Selecciona un departamento, provincia y distrito válidos");
            }
            return ubigeoRepository.findPeruLocation(ubigeoCod)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Selecciona un departamento, provincia y distrito válidos"
                    ));
        }

        if (request.StateId == null || request.StateId <= 0
                || request.CityId == null || request.CityId <= 0) {
            throw new IllegalArgumentException("Selecciona un estado y una ciudad válidos");
        }
        return locationRepository.findForeignLocation(
                countryCod,
                request.StateId,
                request.CityId
        ).orElseThrow(() -> new IllegalArgumentException(
                "El país, estado y ciudad seleccionados no guardan relación"
        ));
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
