package com.ccadmin.app.delivery.service;

import com.ccadmin.app.delivery.model.dto.UbigeoOptionDto;
import com.ccadmin.app.sale.model.entity.ClientAddressEntity;
import com.ccadmin.app.sale.repository.ClientAddressRepository;
import com.ccadmin.app.shared.repository.UbigeoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClientAddressDeliverySearchService {

    private final ClientDeliveryContextService clientDeliveryContextService;
    private final ClientAddressRepository clientAddressRepository;
    private final UbigeoRepository ubigeoRepository;

    public ClientAddressDeliverySearchService(
            ClientDeliveryContextService clientDeliveryContextService,
            ClientAddressRepository clientAddressRepository,
            UbigeoRepository ubigeoRepository
    ) {
        this.clientDeliveryContextService = clientDeliveryContextService;
        this.clientAddressRepository = clientAddressRepository;
        this.ubigeoRepository = ubigeoRepository;
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

    public List<UbigeoOptionDto> findDepartments() {
        return ubigeoRepository.findDepartments().stream()
                .map(UbigeoOptionDto::new)
                .toList();
    }

    public List<UbigeoOptionDto> findProvinces(String departmentCod) {
        if (departmentCod == null || !departmentCod.matches("^\\d{2}$")) {
            throw new IllegalArgumentException("El codigo de departamento debe tener 2 digitos");
        }
        return ubigeoRepository.findProvinces(departmentCod).stream()
                .map(UbigeoOptionDto::new)
                .toList();
    }

    public List<UbigeoOptionDto> findDistricts(String provinceCod) {
        if (provinceCod == null || !provinceCod.matches("^\\d{4}$")) {
            throw new IllegalArgumentException("El codigo de provincia debe tener 4 digitos");
        }
        return ubigeoRepository.findDistricts(provinceCod).stream()
                .map(UbigeoOptionDto::new)
                .toList();
    }
}
