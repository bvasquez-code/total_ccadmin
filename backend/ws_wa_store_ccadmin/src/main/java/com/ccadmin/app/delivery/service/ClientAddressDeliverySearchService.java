package com.ccadmin.app.delivery.service;

import com.ccadmin.app.sale.model.entity.ClientAddressEntity;
import com.ccadmin.app.sale.repository.ClientAddressRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClientAddressDeliverySearchService {

    private final ClientDeliveryContextService clientDeliveryContextService;
    private final ClientAddressRepository clientAddressRepository;

    public ClientAddressDeliverySearchService(
            ClientDeliveryContextService clientDeliveryContextService,
            ClientAddressRepository clientAddressRepository
    ) {
        this.clientDeliveryContextService = clientDeliveryContextService;
        this.clientAddressRepository = clientAddressRepository;
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
}
