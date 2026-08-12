package com.ccadmin.app.delivery.service;

import com.ccadmin.app.delivery.model.dto.LocationOptionDto;
import com.ccadmin.app.sale.model.entity.ClientAddressEntity;
import com.ccadmin.app.sale.repository.ClientAddressRepository;
import com.ccadmin.app.shared.repository.LocationRepository;
import com.ccadmin.app.shared.repository.UbigeoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClientAddressDeliverySearchService {

    private final ClientDeliveryContextService clientDeliveryContextService;
    private final ClientAddressRepository clientAddressRepository;
    private final UbigeoRepository ubigeoRepository;
    private final LocationRepository locationRepository;

    public ClientAddressDeliverySearchService(
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

    public List<ClientAddressEntity> findAll() {
        String clientCod = clientDeliveryContextService.getCurrentClient().ClientCod;
        return clientAddressRepository.findActiveByClientCod(clientCod);
    }

    public ClientAddressEntity findActiveById(String clientCod, Long clientAddressId) {
        if (clientAddressId == null) {
            throw new IllegalArgumentException("Selecciona una dirección de entrega");
        }
        return clientAddressRepository.findActiveByClientAddressIdAndClientCod(
                clientAddressId,
                clientCod
        ).orElseThrow(() -> new IllegalArgumentException(
                "La dirección seleccionada no pertenece al cliente o ya no está disponible"
        ));
    }

    public List<LocationOptionDto> findDepartments() {
        return ubigeoRepository.findDepartments().stream()
                .map(LocationOptionDto::new)
                .toList();
    }

    public List<LocationOptionDto> findCountries() {
        return locationRepository.findCountries().stream()
                .map(LocationOptionDto::new)
                .toList();
    }

    public List<LocationOptionDto> findStates(String countryCod) {
        if (countryCod == null || !countryCod.matches("^[A-Za-z]{3}$")) {
            throw new IllegalArgumentException("El código de país debe tener 3 letras");
        }
        return locationRepository.findStates(countryCod.toUpperCase()).stream()
                .map(LocationOptionDto::new)
                .toList();
    }

    public List<LocationOptionDto> findCities(Long stateId) {
        if (stateId == null || stateId <= 0) {
            throw new IllegalArgumentException("El estado seleccionado no es válido");
        }
        return locationRepository.findCities(stateId).stream()
                .map(LocationOptionDto::new)
                .toList();
    }

    public LocationOptionDto findPeruProvinceLocation(String provinceCod) {
        if (provinceCod == null || !provinceCod.matches("^\\d{4}$")) {
            throw new IllegalArgumentException("El código de provincia debe tener 4 dígitos");
        }
        return locationRepository.findPeruProvinceLocation(provinceCod)
                .map(LocationOptionDto::new)
                .orElse(null);
    }

    public List<LocationOptionDto> findProvinces(String departmentCod) {
        if (departmentCod == null || !departmentCod.matches("^\\d{2}$")) {
            throw new IllegalArgumentException("El codigo de departamento debe tener 2 digitos");
        }
        return ubigeoRepository.findProvinces(departmentCod).stream()
                .map(LocationOptionDto::new)
                .toList();
    }

    public List<LocationOptionDto> findDistricts(String provinceCod) {
        if (provinceCod == null || !provinceCod.matches("^\\d{4}$")) {
            throw new IllegalArgumentException("El codigo de provincia debe tener 4 digitos");
        }
        return ubigeoRepository.findDistricts(provinceCod).stream()
                .map(LocationOptionDto::new)
                .toList();
    }
}
