package com.ccadmin.app.delivery.service;

import com.ccadmin.app.sale.model.entity.ClientAddressEntity;
import com.ccadmin.app.sale.repository.ClientAddressRepository;
import com.ccadmin.app.shared.model.dto.LocationOptionDto;
import com.ccadmin.app.shared.repository.LocationRepository;
import com.ccadmin.app.shared.service.UbigeoSearchService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClientAddressDeliverySearchService {

    private final ClientDeliveryContextService clientDeliveryContextService;
    private final ClientAddressRepository clientAddressRepository;
    private final LocationRepository locationRepository;
    private final UbigeoSearchService ubigeoSearchService;

    public ClientAddressDeliverySearchService(
            ClientDeliveryContextService clientDeliveryContextService,
            ClientAddressRepository clientAddressRepository,
            LocationRepository locationRepository,
            UbigeoSearchService ubigeoSearchService
    ) {
        this.clientDeliveryContextService = clientDeliveryContextService;
        this.clientAddressRepository = clientAddressRepository;
        this.locationRepository = locationRepository;
        this.ubigeoSearchService = ubigeoSearchService;
    }

    public List<ClientAddressEntity> findAllForCurrentClient() {
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
        return ubigeoSearchService.findDepartments();
    }

    public List<LocationOptionDto> findCountries() {
        return ubigeoSearchService.findCountries();
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
        return ubigeoSearchService.findProvinces(departmentCod);
    }

    public List<LocationOptionDto> findDistricts(String provinceCod) {
        return ubigeoSearchService.findDistricts(provinceCod);
    }
}
